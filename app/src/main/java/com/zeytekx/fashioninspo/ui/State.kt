package com.zeytekx.fashioninspo.ui

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zeytekx.fashioninspo.data.Photo
import com.zeytekx.fashioninspo.data.Store
import com.zeytekx.fashioninspo.data.WishItem

enum class Tab(val label: String) { LOOKBOOK("Lookbook"), RAFALE("Rafale"), TAGS("Tags"), WISHLIST("Wishlist"), BILAN("Bilan") }
enum class Access { NONE, PARTIAL, FULL }

data class Raf(val mode: String, val list: List<Photo>, val i: Int = 0, val label: String = "")

sealed interface Sheet {
    data object Menu : Sheet
    data object Albums : Sheet
    data object Families : Sheet
    data class TagActions(val tag: String) : Sheet
    data class BatchAdd(val keys: List<String>) : Sheet
    data class BatchRemove(val keys: List<String>) : Sheet
    data class EditItem(val item: WishItem?, val preset: List<String> = emptyList(), val url: String = "", val name: String = "") : Sheet
    data class Notes(val text: String) : Sheet
    data class Confirm(val title: String, val msg: String, val ok: String, val action: () -> Unit) : Sheet
}

sealed interface Incoming {
    data class Images(val uris: List<Uri>) : Incoming
}

object UI {
    var tab by mutableStateOf(Tab.LOOKBOOK)
    var photos by mutableStateOf<List<Photo>>(emptyList())
    var photosLoaded by mutableStateOf(false)
    var refresh by mutableIntStateOf(0)

    var fAll by mutableStateOf<List<String>>(emptyList())
    var fAny by mutableStateOf<Pair<String, List<String>>?>(null)
    var fUntagged by mutableStateOf(false)

    var selecting by mutableStateOf(false)
    var sel by mutableStateOf<Set<Long>>(emptySet())

    var viewer by mutableStateOf<Pair<List<Photo>, Int>?>(null)
    var raf by mutableStateOf<Raf?>(null)
    var sheet by mutableStateOf<Sheet?>(null)
    var incoming by mutableStateOf<Incoming?>(null)
    var toast by mutableStateOf<String?>(null)
    var wishCat by mutableStateOf<String?>(null)

    var doExport: () -> Unit = {}
    var doImport: () -> Unit = {}

    fun showTag(t: String) {
        fAll = listOf(t); fAny = null; fUntagged = false
        selecting = false; sel = emptySet()
        tab = Tab.LOOKBOOK
    }

    fun openSharedText(t: String, subject: String?) {
        val url = Regex("https?://\\S+").find(t)?.value
        val lines = t.lines().filter { it.isNotBlank() }
        tab = Tab.WISHLIST
        sheet = if (url != null && lines.size <= 3) {
            val name = (subject ?: t.replace(url, ""))
                .replace(Regex("\\s+"), " ").trim().trim('-', '–', '|', ':', ' ').take(80)
            Sheet.EditItem(null, url = url, name = name)
        } else Sheet.Notes(t)
    }
}

fun filteredPhotos(): List<Photo> {
    val tm = Store.tagMap
    var l = UI.photos
    if (UI.fUntagged) l = l.filter { !tm.containsKey(it.key) }
    val all = UI.fAll
    if (all.isNotEmpty()) l = l.filter { p -> val t = tm[p.key]; t != null && all.all { it in t } }
    UI.fAny?.let { (_, tags) ->
        val s = tags.toSet()
        l = l.filter { p -> tm[p.key]?.any { it in s } == true }
    }
    return l
}

fun buildRaf(mode: String): Raf = when (mode) {
    "untagged" -> Raf(mode, UI.photos.filter { !Store.tagMap.containsKey(it.key) })
    "filter" -> Raf(mode, filteredPhotos())
    else -> Raf("all", UI.photos)
}

fun inspoPhotos(tags: List<String>): List<Photo> {
    if (tags.isEmpty()) return emptyList()
    val s = tags.toSet()
    val tm = Store.tagMap
    return UI.photos.filter { p -> tm[p.key]?.any { it in s } == true }
}

fun taggedCount(): Int {
    val tm = Store.tagMap
    return UI.photos.count { tm.containsKey(it.key) }
}

/** Niveau de 1 à 99 selon la part de photos taguées. */
fun level(tagged: Int, total: Int): Int = if (total == 0) 1 else (1 + (tagged.toDouble() / total * 98).toInt()).coerceIn(1, 99)
