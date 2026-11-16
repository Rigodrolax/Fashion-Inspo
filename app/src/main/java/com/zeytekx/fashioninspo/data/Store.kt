package com.zeytekx.fashioninspo.data

import android.content.Context
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class WishItem(
    val id: String,
    val name: String,
    val cat: String,
    val price: String = "",
    val url: String = "",
    val prio: Int = 2,
    val status: String = "envie",
    val tags: List<String> = emptyList(),
    val note: String = "",
)

val DEFAULT_CATS = listOf("Accessoires", "Outerwear", "Chaussures / bottes", "Cravates")
val DEFAULT_FAMS = listOf("Pièces", "Combos & styles", "Muses", "Couleurs & matières")

fun normTag(s: String): String = s.lowercase().trim().trimStart('#').replace(Regex("\\s+"), "")
fun newId(): String = java.lang.Long.toString(System.currentTimeMillis(), 36) + (1000..9999).random()

/** Toutes les données de l'utilisateur, gardées sur le téléphone dans data.json. */
object Store {
    var tagMap by mutableStateOf<Map<String, List<String>>>(emptyMap())
        private set
    var cats by mutableStateOf(DEFAULT_CATS)
    var items by mutableStateOf<List<WishItem>>(emptyList())
    var fams by mutableStateOf(DEFAULT_FAMS)
    var famOf by mutableStateOf<Map<String, String>>(emptyMap())
    var albums by mutableStateOf<Set<Long>>(emptySet())
    var recent by mutableStateOf<List<String>>(emptyList())

    val index: Map<String, Set<String>> by derivedStateOf<Map<String, Set<String>>> {
        val m = HashMap<String, MutableSet<String>>()
        for ((k, ts) in tagMap) for (t in ts) m.getOrPut(t) { HashSet() }.add(k)
        m
    }

    fun count(t: String): Int = index[t]?.size ?: 0
    fun tagsOf(k: String): List<String> = tagMap[k] ?: emptyList()
    fun allTags(): List<String> =
        (index.keys + famOf.keys).distinct()
            .sortedWith(compareByDescending<String> { count(it) }.thenBy { it })

    // ---------- tags ----------
    private fun editTags(block: (MutableMap<String, List<String>>) -> Unit) {
        val m = HashMap(tagMap)
        block(m)
        tagMap = m
        save()
    }

    fun setTags(k: String, list: List<String>) {
        val next = list.map(::normTag).filter { it.isNotEmpty() }.distinct()
        editTags { m -> if (next.isEmpty()) m.remove(k) else m[k] = next }
    }

    fun addTag(k: String, t: String) = addTagTo(listOf(k), t)
    fun removeTag(k: String, t: String) = removeTagFrom(listOf(k), t)

    fun addTagTo(keys: Collection<String>, t: String) {
        val n = normTag(t)
        if (n.isEmpty()) return
        editTags { m ->
            for (k in keys) {
                val c = m[k] ?: emptyList()
                if (n !in c) m[k] = c + n
            }
        }
        pushRecent(n)
    }

    fun removeTagFrom(keys: Collection<String>, t: String) {
        editTags { m ->
            for (k in keys) {
                val c = m[k] ?: continue
                val r = c - t
                if (r.isEmpty()) m.remove(k) else m[k] = r
            }
        }
    }

    /** Renomme un tag partout. Si [to] existe déjà, les deux tags fusionnent. [to] = null supprime le tag. */
    fun renameTag(from: String, to: String?) {
        val target = to?.let(::normTag)?.takeIf { it.isNotEmpty() }
        editTags { m ->
            for ((k, ts) in m.entries.toList()) {
                if (from in ts) {
                    val r = ts.mapNotNull { if (it == from) target else it }.distinct()
                    if (r.isEmpty()) m.remove(k) else m[k] = r
                }
            }
        }
        val f = famOf[from]
        if (f != null) {
            var nf = famOf - from
            if (target != null && target !in nf) nf = nf + (target to f)
            famOf = nf
        }
        items = items.map { item ->
            if (from in item.tags) item.copy(tags = item.tags.mapNotNull { if (it == from) target else it }.distinct()) else item
        }
        recent = recent - from
        save()
    }

    fun pushRecent(t: String) {
        recent = (listOf(t) + recent.filter { it != t }).take(8)
    }

    fun setFamily(tag: String, fam: String?) {
        famOf = if (fam == null) famOf - tag else famOf + (tag to fam)
        save()
    }

    // ---------- wishlist ----------
    fun upsert(item: WishItem) {
        if (item.cat !in cats) cats = cats + item.cat
        items = if (items.any { it.id == item.id }) items.map { if (it.id == item.id) item else it } else items + item
        save()
    }

    fun deleteItem(id: String) {
        items = items.filter { it.id != id }
        save()
    }

    fun importNotes(list: List<NoteItem>): Int {
        val lower = cats.associateBy { it.lowercase() }.toMutableMap()
        val newCats = cats.toMutableList()
        val newItems = items.toMutableList()
        var n = 0
        for (p in list) {
            val c = lower[p.cat.lowercase()] ?: p.cat.also { newCats.add(it); lower[it.lowercase()] = it }
            if (newItems.any { it.cat == c && it.name.equals(p.name, ignoreCase = true) }) continue
            newItems.add(WishItem(id = newId(), name = p.name, cat = c, price = p.price, url = p.url))
            n++
        }
        cats = newCats
        items = newItems
        save()
        return n
    }

    // ---------- sauvegarde ----------
    private var file: File? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var saveJob: Job? = null

    fun init(ctx: Context) {
        if (file != null) return
        val f = File(ctx.filesDir, "data.json")
        file = f
        if (f.exists()) runCatching { fromJson(JSONObject(f.readText())) }
    }

    fun save() {
        val f = file ?: return
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(600)
            val txt = toJson().toString()
            withContext(Dispatchers.IO) { write(f, txt) }
        }
    }

    fun saveNow() {
        val f = file ?: return
        saveJob?.cancel()
        runCatching { write(f, toJson().toString()) }
    }

    private fun write(f: File, txt: String) {
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(txt)
        if (!tmp.renameTo(f)) {
            f.writeText(txt)
            tmp.delete()
        }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("app", "fashion-inspo")
        put("v", 1)
        put("tags", JSONObject().also { o -> tagMap.forEach { (k, ts) -> o.put(k, JSONArray(ts)) } })
        put("cats", JSONArray(cats))
        put("fams", JSONArray(fams))
        put("famOf", JSONObject().also { o -> famOf.forEach { (k, v) -> o.put(k, v) } })
        put("albums", JSONArray(albums.toList()))
        put("items", JSONArray().also { a ->
            items.forEach {
                a.put(JSONObject().apply {
                    put("id", it.id); put("name", it.name); put("cat", it.cat)
                    put("price", it.price); put("url", it.url); put("prio", it.prio)
                    put("status", it.status); put("tags", JSONArray(it.tags)); put("note", it.note)
                })
            }
        })
    }

    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

    fun fromJson(o: JSONObject) {
        o.optJSONObject("tags")?.let { t ->
            val m = HashMap<String, List<String>>()
            val it = t.keys()
            while (it.hasNext()) {
                val k = it.next()
                val a = t.optJSONArray(k) ?: continue
                val l = a.strings().filter { s -> s.isNotEmpty() }
                if (l.isNotEmpty()) m[k] = l
            }
            tagMap = m
        }
        o.optJSONArray("cats")?.let { cats = it.strings() }
        o.optJSONArray("fams")?.let { fams = it.strings() }
        o.optJSONObject("famOf")?.let { f ->
            val m = HashMap<String, String>()
            val it = f.keys()
            while (it.hasNext()) { val k = it.next(); m[k] = f.optString(k) }
            famOf = m
        }
        o.optJSONArray("albums")?.let { a -> albums = (0 until a.length()).map { a.getLong(it) }.toSet() }
        o.optJSONArray("items")?.let { a ->
            items = (0 until a.length()).mapNotNull { i ->
                val j = a.optJSONObject(i) ?: return@mapNotNull null
                WishItem(
                    id = j.optString("id").ifEmpty { newId() },
                    name = j.optString("name"),
                    cat = j.optString("cat").ifEmpty { "Divers" },
                    price = j.optString("price"),
                    url = j.optString("url"),
                    prio = j.optInt("prio", 2).coerceIn(1, 3),
                    status = j.optString("status", "envie").ifEmpty { "envie" },
                    tags = j.optJSONArray("tags")?.strings() ?: emptyList(),
                    note = j.optString("note"),
                )
            }
        }
    }

    fun restore(o: JSONObject) {
        fromJson(o)
        save()
    }
}
