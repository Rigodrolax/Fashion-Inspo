@file:OptIn(ExperimentalLayoutApi::class)

package com.zeytekx.fashioninspo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeytekx.fashioninspo.data.Store
import com.zeytekx.fashioninspo.data.normTag

@Composable
fun TagsScreen() {
    var q by remember { mutableStateOf("") }
    val nq = normTag(q)
    val tags = remember(Store.tagMap, Store.famOf, nq) { Store.allTags().filter { nq.isEmpty() || nq in it } }
    val max = (tags.maxOfOrNull { Store.count(it) } ?: 1).coerceAtLeast(1)
    val groups: List<String?> = Store.fams + listOf(null)
    fun groupOf(t: String): String? = Store.famOf[t]?.takeIf { it in Store.fams }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FTitle("Tes tags", Modifier.weight(1f), big = true)
                    Btn("Familles") { UI.sheet = Sheet.Families }
                }
                FField(q, { q = it }, "Chercher un tag…", cap = KeyboardCapitalization.None)
                Text("Touche un tag pour le ranger dans une famille, le renommer ou le fusionner avec un autre.", fontSize = 14.sp, color = FF.Mute)
            }
        }
        if (tags.isEmpty()) {
            item {
                Text(
                    if (Store.allTags().isEmpty()) "Pas encore de tags : ils apparaîtront ici dès que tu tagueras des photos." else "Aucun tag ne contient « $nq ».",
                    Modifier.padding(vertical = 20.dp), color = FF.Mute,
                )
            }
        }
        for (g in groups) {
            val ts = tags.filter { groupOf(it) == g }
            if (ts.isEmpty()) continue
            item(key = "h_$g") {
                Row(Modifier.padding(top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Orb(famColor(g), 16.dp)
                    Spacer(Modifier.width(8.dp))
                    Text((g ?: "À ranger").uppercase(), style = TitleStyle.copy(fontSize = 16.sp), modifier = Modifier.weight(1f))
                    Text("${ts.size} tags", fontSize = 13.sp, color = FF.Mute)
                }
            }
            items(ts, key = { "t_$it" }) { t -> TagRow(t, max) }
        }
    }
}

@Composable
private fun TagRow(t: String, max: Int) {
    val n = Store.count(t)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0x30000000))
            .clickable { UI.sheet = Sheet.TagActions(t) }.padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(t, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Box(Modifier.width(70.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF050A20))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(n.toFloat() / max).background(Brush.horizontalGradient(listOf(FF.Crystal, FF.Gold))))
        }
        Text(fmt(n), Modifier.width(56.dp), color = FF.Mute, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable
fun TagActionsSheet(t: String, close: () -> Unit) {
    var name by remember { mutableStateOf(t) }
    val n = Store.count(t)
    SheetFrame(t, close) {
        Text("${fmt(n)} photo${if (n > 1) "s" else ""}", color = FF.Mute)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("Voir les photos", Modifier.weight(1f), enabled = n > 0) { close(); UI.showTag(t) }
            Btn("Vérifier en rafale", Modifier.weight(1f), enabled = n > 0) {
                close(); UI.showTag(t); UI.raf = buildRaf("filter").copy(mode = "list", label = "pour « $t »"); UI.tab = Tab.RAFALE
            }
        }
        FLabel("Famille")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Store.fams.forEach { f ->
                val on = Store.famOf[t] == f
                FChip(f, on = on, dot = famColor(f)) { Store.setFamily(t, if (on) null else f) }
            }
        }
        FLabel("Renommer ou fusionner")
        FField(name, { name = it }, "Nouveau nom", cap = KeyboardCapitalization.None)
        val target = normTag(name)
        Text(
            when {
                target.isEmpty() || target == t -> "Si tu choisis le nom d'un tag existant, les deux tags seront fusionnés."
                Store.count(target) > 0 -> "« $target » existe déjà : les ${fmt(n)} photos le recevront et « $t » disparaîtra."
                else -> "« $t » deviendra « $target » sur ${fmt(n)} photos."
            },
            fontSize = 14.sp, color = FF.Mute,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("Renommer", Modifier.weight(1f), primary = true, enabled = target.isNotEmpty() && target != t) {
                Store.renameTag(t, target)
                UI.fAll = UI.fAll.map { if (it == t) target else it }.distinct()
                UI.toast = "« $t » → « $target »"
                close()
            }
            Btn("Supprimer", Modifier.weight(1f), danger = true) {
                UI.sheet = Sheet.Confirm("Supprimer « $t » ?", "Le tag sera retiré de ${fmt(n)} photos. Les photos elles-mêmes ne sont pas touchées.", "Supprimer") {
                    Store.renameTag(t, null)
                    UI.fAll = UI.fAll - t
                    UI.toast = "Tag supprimé"
                }
            }
        }
    }
}
