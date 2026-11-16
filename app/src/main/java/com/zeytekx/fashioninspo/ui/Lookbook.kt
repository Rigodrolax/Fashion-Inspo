@file:OptIn(ExperimentalLayoutApi::class)

package com.zeytekx.fashioninspo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zeytekx.fashioninspo.data.Photo
import com.zeytekx.fashioninspo.data.Store

@Composable
fun LookbookScreen() {
    val list = remember(UI.photos, Store.tagMap, UI.fAll, UI.fAny, UI.fUntagged) { filteredPhotos() }
    val top = remember(Store.tagMap, UI.fAll) { Store.allTags().filter { Store.count(it) > 0 && it !in UI.fAll }.take(30) }
    BackHandler(enabled = UI.selecting) { UI.selecting = false; UI.sel = emptySet() }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TagInput("Filtrer par tag…", create = false, exclude = UI.fAll) { UI.fAll = UI.fAll + it }
            if (UI.fAll.isNotEmpty() || UI.fAny != null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    UI.fAll.forEach { t -> FChip(t, on = true, removable = true) { UI.fAll = UI.fAll - t } }
                    UI.fAny?.let { FChip("Wishlist : ${it.first}", on = true, removable = true) { UI.fAny = null } }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { FChip("Non taguées", on = UI.fUntagged) { UI.fUntagged = !UI.fUntagged } }
                items(top, key = { it }) { t -> FChip(t, count = Store.count(t), dot = famColor(Store.famOf[t]).takeIf { Store.famOf[t] != null }) { UI.fAll = UI.fAll + t } }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${fmt(list.size)} photo${if (list.size > 1) "s" else ""}", Modifier.weight(1f), color = FF.Mute, fontSize = 15.sp)
                Btn(if (UI.selecting) "Terminer" else "Sélection") { UI.selecting = !UI.selecting; UI.sel = emptySet() }
            }
        }
        if (!UI.photosLoaded) {
            FEmpty("Lecture de ta galerie…")
        } else if (list.isEmpty()) {
            FEmpty(if (UI.photos.isEmpty()) "Aucune photo dans les albums suivis. Ajoute un album depuis le Menu." else "Aucune photo ne correspond à ce filtre.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(108.dp),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                itemsIndexed(list, key = { _, p -> p.id }) { i, p ->
                    PhotoCell(
                        p, selected = p.id in UI.sel,
                        onTap = {
                            if (UI.selecting) UI.sel = if (p.id in UI.sel) UI.sel - p.id else UI.sel + p.id
                            else UI.viewer = list to i
                        },
                        onLong = { if (!UI.selecting) { UI.selecting = true; UI.sel = setOf(p.id) } },
                    )
                }
            }
        }
        if (UI.selecting) SelBar(list)
    }
}

@Composable
private fun PhotoCell(p: Photo, selected: Boolean, onTap: () -> Unit, onLong: () -> Unit) {
    val n = Store.tagsOf(p.key).size
    val tap by rememberUpdatedState(onTap)
    val long by rememberUpdatedState(onLong)
    Box(
        Modifier
            .aspectRatio(0.75f)
            .background(FF.WinBot)
            .then(if (selected) Modifier.border(3.dp, FF.Gold) else Modifier)
            .pointerInput(p.id) { detectTapGestures(onTap = { tap() }, onLongPress = { long() }) }
    ) {
        AsyncImage(p.uri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Text(
            if (n > 0) "$n" else "•",
            Modifier.align(Alignment.BottomEnd).padding(4.dp)
                .background(Color(0xCC050A20), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp),
            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (n > 0) FF.Ink else FF.Gold,
        )
        if (selected) Box(Modifier.align(Alignment.TopStart).padding(6.dp)) { Cursor(16.dp) }
    }
}

@Composable
private fun SelBar(list: List<Photo>) {
    val keys = { list.filter { it.id in UI.sel }.map { it.key } }
    Win(Modifier.fillMaxWidth().padding(horizontal = 6.dp), gold = true, pad = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${fmt(UI.sel.size)} sélectionnée${if (UI.sel.size > 1) "s" else ""}", Modifier.weight(1f), fontWeight = FontWeight.Bold)
            val allOn = UI.sel.size == list.size
            Btn(if (allOn) "Aucune" else "Toutes") { UI.sel = if (allOn) emptySet() else list.map { it.id }.toSet() }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("Retirer un tag", Modifier.weight(1f), enabled = UI.sel.isNotEmpty()) { UI.sheet = Sheet.BatchRemove(keys()) }
            Btn("Taguer", Modifier.weight(1f), primary = true, enabled = UI.sel.isNotEmpty()) { UI.sheet = Sheet.BatchAdd(keys()) }
        }
    }
}

@Composable
fun TagsEditor(key: String) {
    val cur = Store.tagsOf(key)
    if (cur.isEmpty()) Text("Aucun tag pour l'instant", color = FF.Mute, fontSize = 15.sp)
    else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        cur.forEach { t -> FChip(t, on = true, removable = true) { Store.removeTag(key, t) } }
    }
    TagInput(exclude = cur) { Store.addTag(key, it) }
}

@Composable
fun Viewer(state: Pair<List<Photo>, Int>) {
    val (list, start) = state
    var i by remember(state) { mutableIntStateOf(start.coerceIn(0, (list.size - 1).coerceAtLeast(0))) }
    BackHandler { UI.viewer = null }
    val p = list.getOrNull(i) ?: return
    Column(
        Modifier.fillMaxSize().background(Color(0xFF020308)).pointerInput(Unit) { detectTapGestures { } }
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("✕", Modifier.clickable { UI.viewer = null }.padding(8.dp), fontSize = 20.sp, color = FF.Mute)
            Text("${fmt(i + 1)} / ${fmt(list.size)}", Modifier.weight(1f), color = FF.Mute)
            Btn("Rafale d'ici") {
                UI.raf = Raf("list", list, i, "depuis le Lookbook")
                UI.viewer = null
                UI.tab = Tab.RAFALE
            }
        }
        Box(
            Modifier.weight(1f).fillMaxWidth().swipe { d -> val n = i + d; if (n in list.indices) i = n },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(p.uri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
        Win(Modifier.fillMaxWidth().heightIn(max = 340.dp).padding(6.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TagsEditor(p.key)
                Text(p.name, fontSize = 13.sp, color = FF.Mute)
            }
        }
    }
}
