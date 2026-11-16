@file:OptIn(ExperimentalLayoutApi::class)

package com.zeytekx.fashioninspo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zeytekx.fashioninspo.data.Store
import java.util.Calendar
import kotlin.math.ceil

@Composable
fun BilanScreen() {
    val total = UI.photos.size
    val tagged = remember(UI.photos, Store.tagMap) { taggedCount() }
    val lv = level(tagged, total)
    val nextAt = if (total == 0 || lv >= 99) 0 else ceil(lv.toDouble() * total / 98).toInt() - tagged

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Win(pad = 16.dp) {
            FLabel("Statut")
            Row(verticalAlignment = Alignment.Bottom) {
                Text("FASHION INSPO", Modifier.weight(1f), style = TitleStyle.copy(fontSize = 18.sp))
                Text("LV ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FF.Crystal)
                Text("$lv", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = FF.Gold)
            }
            Spacer(Modifier.height(6.dp))
            Row {
                Text("Photos taguées", Modifier.weight(1f), color = FF.Mute)
                Text("${fmt(tagged)} / ${fmt(total)}", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Gauge(if (total == 0) 0f else tagged.toFloat() / total)
            Spacer(Modifier.height(6.dp))
            Text(
                if (lv >= 99) "Niveau maximum atteint." else "Prochain niveau dans ${fmt(nextAt.coerceAtLeast(1))} photo${if (nextAt > 1) "s" else ""}",
                fontSize = 14.sp, color = FF.Mute,
            )
            if (total - tagged > 0) {
                Spacer(Modifier.height(12.dp))
                Btn("Continuer en rafale (${fmt(total - tagged)})", Modifier.fillMaxWidth(), primary = true) {
                    UI.raf = buildRaf("untagged"); UI.tab = Tab.RAFALE
                }
            }
        }

        // Look du jour
        val taggedPhotos = remember(UI.photos, Store.tagMap) { UI.photos.filter { Store.tagMap.containsKey(it.key) } }
        if (taggedPhotos.isNotEmpty()) {
            val cal = Calendar.getInstance()
            val seed = cal.get(Calendar.YEAR) * 400 + cal.get(Calendar.DAY_OF_YEAR)
            val idx = ((seed.toLong() * 2654435761L) and 0x7fffffffL) % taggedPhotos.size
            val p = taggedPhotos[idx.toInt()]
            Win {
                FTitle("Look du jour")
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    p.uri, null,
                    Modifier.fillMaxWidth().height(380.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF020308))
                        .clickable { UI.viewer = listOf(p) to 0 },
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Store.tagsOf(p.key).forEach { t -> FChip(t) { UI.showTag(t) } }
                }
            }
        }

        // Inspirations absentes de la wishlist
        val gaps = remember(Store.tagMap, Store.items, Store.famOf) {
            val linked = Store.items.flatMap { it.tags }.toSet()
            Store.allTags().filter { Store.count(it) >= 5 && it !in linked && Store.famOf[it] != "Muses" }.take(8)
        }
        if (gaps.isNotEmpty()) {
            Win {
                FTitle("Beaucoup d'inspiration, rien dans la wishlist")
                Text("Tes tags fréquents qu'aucun article de ta wishlist ne reprend.", fontSize = 14.sp, color = FF.Mute)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    gaps.forEach { t ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0x33000000)).padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Orb(famColor(Store.famOf[t]), 12.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(t, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Text("${fmt(Store.count(t))}  ", color = FF.Mute, fontSize = 14.sp)
                            Btn("+ Wishlist") { UI.tab = Tab.WISHLIST; UI.sheet = Sheet.EditItem(null, preset = listOf(t)) }
                        }
                    }
                }
            }
        }

        // Wishlist classée par inspirations
        val ranked = remember(Store.tagMap, Store.items, UI.photos) {
            Store.items.filter { it.status != "achete" && it.tags.isNotEmpty() }
                .map { it to inspoPhotos(it.tags).size }.sortedByDescending { it.second }.take(10)
        }
        if (ranked.isNotEmpty()) {
            Win {
                FTitle("Ce que tu veux le plus")
                Text("Articles de ta wishlist classés selon le nombre de photos qui les illustrent.", fontSize = 14.sp, color = FF.Mute)
                Spacer(Modifier.height(8.dp))
                val max = ranked.first().second.coerceAtLeast(1)
                ranked.forEach { (item, n) -> Bar(item.name, n, max) { UI.sheet = Sheet.EditItem(item) } }
            }
        }

        // Tags les plus présents
        val top = remember(Store.tagMap) { Store.allTags().filter { Store.count(it) > 0 }.take(12) }
        if (top.isNotEmpty()) {
            Win {
                FTitle("Tes tags les plus présents")
                Spacer(Modifier.height(8.dp))
                val max = Store.count(top.first()).coerceAtLeast(1)
                top.forEach { t -> Bar(t, Store.count(t), max, famColor(Store.famOf[t])) { UI.showTag(t) } }
            }
        }
    }
}

@Composable
private fun Bar(label: String, n: Int, max: Int, dot: Color? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        if (dot != null) {
            Orb(dot, 10.dp)
            Spacer(Modifier.width(6.dp))
        }
        Text(label, Modifier.weight(1f), maxLines = 1, fontSize = 15.sp)
        Box(Modifier.weight(1.3f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF050A20))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(n.toFloat() / max).background(Brush.horizontalGradient(listOf(FF.Crystal, FF.Gold))))
        }
        Text(fmt(n), Modifier.width(52.dp), color = FF.Mute, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}
