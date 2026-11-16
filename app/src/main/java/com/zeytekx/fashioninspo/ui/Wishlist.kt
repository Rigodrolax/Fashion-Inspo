@file:OptIn(ExperimentalLayoutApi::class)

package com.zeytekx.fashioninspo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zeytekx.fashioninspo.data.Store
import com.zeytekx.fashioninspo.data.WishItem
import com.zeytekx.fashioninspo.data.newId
import com.zeytekx.fashioninspo.data.parseNotes

val STATUS = listOf("envie" to "Envie", "repere" to "Repéré", "achete" to "Acheté")
fun statusColor(s: String): Color = when (s) { "repere" -> FF.Gold; "achete" -> FF.Green; else -> FF.Mute }
fun statusLabel(s: String): String = STATUS.firstOrNull { it.first == s }?.second ?: "Envie"

@Composable
fun WishlistScreen() {
    val cat = UI.wishCat?.takeIf { it in Store.cats }
    val items = remember(Store.items, cat) {
        Store.items.filter { cat == null || it.cat == cat }
            .sortedWith(compareBy<WishItem> { if (it.status == "achete") 1 else 0 }.thenByDescending { it.prio }.thenBy { it.name.lowercase() })
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FTitle("Wishlist", Modifier.weight(1f), big = true)
                Btn("Notes") { UI.sheet = Sheet.Notes("") }
                Btn("+ Article", primary = true) { UI.sheet = Sheet.EditItem(null) }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { FChip("Tout", on = cat == null, count = Store.items.size) { UI.wishCat = null } }
                items(Store.cats, key = { it }) { c ->
                    FChip(c, on = cat == c, count = Store.items.count { it.cat == c }) { UI.wishCat = c }
                }
            }
        }
        if (Store.items.isEmpty()) {
            item {
                Win(pad = 16.dp) {
                    Text("Ta wishlist est vide. Copie le texte de ta note, puis touche « Coller mes notes » : catégories et articles seront repris tels quels.")
                    Spacer(Modifier.height(12.dp))
                    Btn("Coller mes notes", primary = true) { UI.sheet = Sheet.Notes("") }
                    Spacer(Modifier.height(8.dp))
                    Text("Tu peux aussi partager ta note directement vers Fashion Inspo depuis Samsung Notes.", fontSize = 14.sp, color = FF.Mute)
                }
            }
        }
        items(items, key = { it.id }) { WishCard(it, showCat = cat == null) }
    }
}

@Composable
private fun WishCard(it: WishItem, showCat: Boolean) {
    val inspo = remember(Store.tagMap, UI.photos, it.tags) { inspoPhotos(it.tags) }
    val uri = LocalUriHandler.current
    val done = it.status == "achete"
    Win(Modifier.fillMaxWidth().clickable { UI.sheet = Sheet.EditItem(it) }, gold = it.prio == 3 && !done) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                it.name, Modifier.weight(1f), fontSize = 19.sp, fontWeight = FontWeight.Bold,
                color = if (done) FF.Mute else FF.Ink, textDecoration = if (done) TextDecoration.LineThrough else null,
            )
            if (it.price.isNotEmpty()) Text(it.price, fontWeight = FontWeight.Bold, color = FF.Gold)
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showCat) Text(it.cat, fontSize = 14.sp, color = FF.Mute)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { repeat(3) { k -> Orb(if (k < it.prio) FF.Gold else Color(0xFF3A4570), 10.dp) } }
            Spacer(Modifier.weight(1f))
            Text(
                statusLabel(it.status).uppercase(),
                Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0x55000000)).clickable {
                    val idx = STATUS.indexOfFirst { s -> s.first == it.status }
                    Store.upsert(it.copy(status = STATUS[(idx + 1) % STATUS.size].first))
                }.padding(horizontal = 8.dp, vertical = 3.dp),
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = statusColor(it.status),
            )
        }
        Spacer(Modifier.height(8.dp))
        if (it.tags.isEmpty()) {
            Text("Relie des tags pour voir tes inspirations.", fontSize = 14.sp, color = FF.Mute)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                it.tags.forEach { t -> FChip(t, count = Store.count(t)) { UI.showTag(t) } }
            }
        }
        if (inspo.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                inspo.take(4).forEach { p ->
                    AsyncImage(p.uri, null, Modifier.weight(1f).aspectRatio(0.75f).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
                }
                repeat(4 - inspo.take(4).size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(6.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (inspo.isNotEmpty()) {
                Text(
                    "Voir les ${fmt(inspo.size)} inspirations ›",
                    Modifier.weight(1f).clickable {
                        UI.fAll = emptyList(); UI.fUntagged = false; UI.fAny = it.name to it.tags; UI.tab = Tab.LOOKBOOK
                    }.padding(vertical = 4.dp),
                    color = FF.Gold, fontWeight = FontWeight.SemiBold,
                )
            } else Spacer(Modifier.weight(1f))
            if (it.url.isNotEmpty()) {
                Text("Lien ›", Modifier.clickable { runCatching { uri.openUri(it.url) } }.padding(4.dp), color = FF.Crystal, fontWeight = FontWeight.SemiBold)
            }
        }
        if (it.note.isNotEmpty()) Text(it.note, fontSize = 14.sp, color = FF.Mute)
    }
}

@Composable
fun EditItemSheet(s: Sheet.EditItem, close: () -> Unit) {
    val it = s.item
    val isNew = it == null
    var name by remember { mutableStateOf(it?.name ?: s.name) }
    var cat by remember { mutableStateOf(it?.cat ?: (UI.wishCat ?: Store.cats.firstOrNull() ?: "Divers")) }
    var newCat by remember { mutableStateOf("") }
    var price by remember { mutableStateOf(it?.price ?: "") }
    var url by remember { mutableStateOf(it?.url ?: s.url) }
    var note by remember { mutableStateOf(it?.note ?: "") }
    var prio by remember { mutableIntStateOf(it?.prio ?: 2) }
    var status by remember { mutableStateOf(it?.status ?: "envie") }
    var tags by remember { mutableStateOf(it?.tags ?: s.preset) }
    val NEW = "\u0000new"
    val inspoN = remember(tags, Store.tagMap) { inspoPhotos(tags).size }

    SheetFrame(if (isNew) "Nouvel article" else "Modifier", close, actions = {
        if (!isNew) Btn("Supprimer", danger = true) {
            UI.sheet = Sheet.Confirm("Supprimer « ${it!!.name} » ?", "L'article sera retiré de ta wishlist.", "Supprimer") { Store.deleteItem(it.id) }
        }
        Btn("Enregistrer", primary = true, enabled = name.isNotBlank()) {
            val c = if (cat == NEW) newCat.trim().ifEmpty { "Divers" } else cat
            Store.upsert(
                WishItem(
                    id = it?.id ?: newId(), name = name.trim(), cat = c, price = price.trim(), url = url.trim(),
                    prio = prio, status = status, tags = tags, note = note.trim(),
                )
            )
            close()
        }
    }) {
        FLabel("Article")
        FField(name, { name = it }, "Ex. Manteau en cuir noir")
        FLabel("Catégorie")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Store.cats.forEach { c -> FChip(c, on = cat == c) { cat = c } }
            FChip("+ Nouvelle", on = cat == NEW) { cat = NEW }
        }
        if (cat == NEW) FField(newCat, { newCat = it }, "Nom de la catégorie")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FLabel("Prix"); FField(price, { price = it }, "250 CHF")
            }
            Column(Modifier.weight(1.6f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FLabel("Lien"); FField(url, { url = it }, "https://…", kb = KeyboardType.Uri, cap = KeyboardCapitalization.None)
            }
        }
        FLabel("Priorité")
        Seg(listOf("1" to "Un jour", "2" to "Bientôt", "3" to "Prioritaire"), prio.toString()) { prio = it.toInt() }
        FLabel("Statut")
        Seg(STATUS, status) { status = it }
        FLabel("Tags reliés")
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tags.forEach { t -> FChip(t, on = true, removable = true) { tags = tags - t } }
            }
        }
        TagInput("Relier un tag (ex. leathercoat)…", exclude = tags) { if (it !in tags) tags = tags + it }
        Text(if (tags.isEmpty()) "Aucun tag relié" else "${fmt(inspoN)} photo${if (inspoN > 1) "s" else ""} d'inspiration", fontSize = 14.sp, color = FF.Mute)
        FLabel("Note")
        FField(note, { note = it }, "Taille, couleur, boutique…")
    }
}

@Composable
fun NotesSheet(initial: String, close: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    val parsed = remember(text) { parseNotes(text) }
    SheetFrame("Coller ma wishlist", close, actions = {
        Btn("Annuler", onClick = close)
        Btn("Ajouter", primary = true, enabled = parsed.isNotEmpty()) {
            val n = Store.importNotes(parsed)
            UI.toast = "${fmt(n)} article${if (n > 1) "s" else ""} ajouté${if (n > 1) "s" else ""}"
            close()
        }
    }) {
        Text("Colle tout le texte de ta note. Une ligne sans puce devient une catégorie, une ligne à puce devient un article. Les prix et les liens sont repérés tout seuls.", fontSize = 14.sp, color = FF.Mute)
        FField(text, { text = it }, "Outerwear\n- Manteau en cuir noir\n- Trench beige\n\nCravates\n- Cravate en tricot bordeaux", singleLine = false, minLines = 6)
        if (parsed.isNotEmpty()) {
            val by = parsed.groupBy { it.cat }
            Text("${fmt(parsed.size)} articles dans ${by.size} catégorie${if (by.size > 1) "s" else ""}", color = FF.Gold, fontWeight = FontWeight.SemiBold)
            by.forEach { (c, ps) ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0x33000000)).padding(10.dp)) {
                    Text(c.uppercase(), fontWeight = FontWeight.Bold, color = FF.Crystal, fontSize = 14.sp)
                    Text(ps.joinToString(" · ") { p -> p.name + if (p.price.isNotEmpty()) " (${p.price})" else "" }, fontSize = 15.sp)
                }
            }
        }
    }
}
