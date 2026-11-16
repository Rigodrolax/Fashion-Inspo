@file:OptIn(ExperimentalLayoutApi::class)

package com.zeytekx.fashioninspo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zeytekx.fashioninspo.data.Store

@Composable
fun RafaleScreen() {
    if (!UI.photosLoaded) { FEmpty("Lecture de ta galerie…"); return }
    if (UI.photos.isEmpty()) { FEmpty("Aucune photo dans les albums suivis. Ajoute un album depuis le Menu."); return }
    LaunchedEffect(Unit) { if (UI.raf == null) UI.raf = buildRaf("untagged") }
    val r = UI.raf ?: return

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Seg(
            listOf("untagged" to "Non taguées", "filter" to "Filtre", "all" to "Toutes"),
            r.mode,
        ) { UI.raf = buildRaf(it) }
        if (r.mode == "list") Text("Série ouverte ${r.label} · ${fmt(r.list.size)} photos", fontSize = 14.sp, color = FF.Mute)

        if (r.list.isEmpty()) {
            FEmpty(if (r.mode == "untagged") "Toutes tes photos ont au moins un tag. Niveau maximum en vue." else "Aucune photo dans cette série.")
        } else {
            RafalePanel(r)
        }
    }
}

@Composable
private fun ColumnScope.RafalePanel(r: Raf) {
        val i = r.i.coerceIn(0, r.list.size - 1)
        val p = r.list[i]
        val move: (Int) -> Unit = { d ->
            val n = i + d
            if (n >= r.list.size) UI.toast = "Fin de la série"
            else if (n >= 0) UI.raf = r.copy(i = n)
        }
        val shape = RoundedCornerShape(8.dp)
        AsyncImage(
            p.uri, null,
            Modifier.weight(1f).fillMaxWidth().clip(shape).background(Color(0xFF020308)).border(2.dp, FF.Border, shape).swipe(move),
            contentScale = ContentScale.Fit,
        )
        Text("Photo ${fmt(i + 1)} sur ${fmt(r.list.size)}  ·  glisse pour changer", fontSize = 14.sp, color = FF.Mute)

        val cur = Store.tagsOf(p.key)
        val quick = remember(Store.tagMap, Store.recent) { (Store.recent + Store.allTags().filter { Store.count(it) > 0 }.take(16)).distinct().take(22) }
        Column(Modifier.heightIn(max = 290.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (cur.isEmpty()) Text("Pas encore de tag", color = FF.Mute, fontSize = 15.sp)
            else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                cur.forEach { t -> FChip(t, on = true, removable = true) { Store.removeTag(p.key, t) } }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                quick.filter { it !in cur }.forEach { t -> FChip(t, dot = Store.famOf[t]?.let { famColor(it) }) { Store.addTag(p.key, t) } }
            }
            TagInput(exclude = cur) { Store.addTag(p.key, it) }
        }
        val prev = if (i > 0) Store.tagsOf(r.list[i - 1].key) else emptyList()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("‹ Préc.", Modifier.weight(1f), enabled = i > 0) { move(-1) }
            Btn("Tags d'avant", Modifier.weight(1.3f), enabled = prev.isNotEmpty()) {
                Store.setTags(p.key, cur + prev)
                prev.forEach { Store.pushRecent(it) }
            }
            Btn("Suiv. ›", Modifier.weight(1f), primary = true) { move(1) }
        }
    }
