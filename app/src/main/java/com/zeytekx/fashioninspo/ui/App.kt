@file:OptIn(ExperimentalLayoutApi::class)

package com.zeytekx.fashioninspo.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zeytekx.fashioninspo.data.Album
import com.zeytekx.fashioninspo.data.Media
import com.zeytekx.fashioninspo.data.OWN_ALBUM
import com.zeytekx.fashioninspo.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun App(access: Access, onRequest: () -> Unit, onSettings: () -> Unit) {
    val ctx = LocalContext.current

    val exportL = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = runCatching {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(Store.toJson().toString(1).toByteArray()) }
            }.isSuccess
            UI.toast = if (ok) "Sauvegarde enregistrée" else "La sauvegarde a échoué"
        }
    }
    val importL = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val ok = runCatching {
                val txt = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("vide")
                Store.restore(JSONObject(txt))
            }.isSuccess
            UI.toast = if (ok) "Données restaurées" else "Ce fichier n'est pas une sauvegarde Fashion Inspo"
            UI.refresh++
        }
    }
    SideEffect {
        UI.doExport = { exportL.launch("fashion-inspo-" + SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(Date()) + ".json") }
        UI.doImport = { importL.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
    }

    val hasAccess = access != Access.NONE
    LaunchedEffect(hasAccess, Store.albums, UI.refresh) {
        if (hasAccess) {
            val l = withContext(Dispatchers.IO) { runCatching { Media.photos(ctx, Store.albums) }.getOrDefault(emptyList()) }
            UI.photos = l
            UI.photosLoaded = true
        }
    }

    Box(Modifier.fillMaxSize().ffBackground()) {
        Column(Modifier.fillMaxSize()) {
            TopBar()
            if (access == Access.PARTIAL) {
                Text(
                    "Accès limité à quelques photos. Touche ici pour autoriser toute la galerie.",
                    Modifier.fillMaxWidth().background(Color(0xFF3A2A1C)).clickable { onSettings() }.padding(10.dp),
                    fontSize = 14.sp, color = FF.Gold,
                )
            }
            Box(Modifier.weight(1f)) {
                when {
                    !hasAccess -> PermissionScreen(onRequest, onSettings)
                    Store.albums.isEmpty() -> FirstRun()
                    else -> when (UI.tab) {
                        Tab.LOOKBOOK -> LookbookScreen()
                        Tab.RAFALE -> RafaleScreen()
                        Tab.TAGS -> TagsScreen()
                        Tab.WISHLIST -> WishlistScreen()
                        Tab.BILAN -> BilanScreen()
                    }
                }
            }
            BottomMenu()
        }
        UI.viewer?.let { Viewer(it) }
        UI.sheet?.let { s -> key(s) { SheetHost(s) } }
        when (val inc = UI.incoming) {
            is Incoming.Images -> key(inc) { ShareInSheet(inc.uris) }
            null -> {}
        }
        UI.toast?.let { msg ->
            LaunchedEffect(msg) { delay(2600); UI.toast = null }
            Box(Modifier.fillMaxWidth().padding(top = 64.dp, start = 16.dp, end = 16.dp), contentAlignment = Alignment.TopCenter) {
                Win(pad = 12.dp, gold = true) { Text(msg, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun TopBar() {
    val total = UI.photos.size
    val tagged = remember(UI.photos, Store.tagMap) { taggedCount() }
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Orb(FF.Crystal, 16.dp)
        Spacer(Modifier.width(10.dp))
        Text("FASHION INSPO", style = TitleStyle.copy(fontSize = 19.sp), modifier = Modifier.weight(1f), maxLines = 1)
        if (total > 0) {
            Text("LV ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FF.Crystal)
            Text("${level(tagged, total)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FF.Gold)
            Spacer(Modifier.width(10.dp))
        }
        Btn("Menu") { UI.sheet = Sheet.Menu }
    }
}

@Composable
private fun BottomMenu() {
    Win(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 6.dp), pad = 4.dp) {
        Row(Modifier.fillMaxWidth()) {
            Tab.entries.forEach { t ->
                val on = UI.tab == t
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).clickable {
                        UI.tab = t; UI.selecting = false; UI.sel = emptySet()
                    }.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (on) {
                        Cursor(7.dp)
                        Spacer(Modifier.width(3.dp))
                    }
                    Text(
                        t.label.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                        color = if (on) FF.Ink else FF.Mute, maxLines = 1, softWrap = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit, onSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        Win(pad = 18.dp) {
            FTitle("Bienvenue", big = true)
            Spacer(Modifier.height(10.dp))
            Text("Fashion Inspo lit directement tes albums pour que tu puisses taguer tes looks. Aucune photo n'est copiée ni envoyée en ligne.")
            Spacer(Modifier.height(16.dp))
            Btn("Autoriser l'accès aux photos", primary = true, onClick = onRequest)
            Spacer(Modifier.height(8.dp))
            Text(
                "Si rien ne s'affiche, ouvre les réglages de l'app.",
                Modifier.clickable { onSettings() }, fontSize = 14.sp, color = FF.Crystal,
            )
        }
    }
}

@Composable
private fun FirstRun() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Win(pad = 16.dp) {
            FTitle("Choisis tes albums", big = true)
            Spacer(Modifier.height(8.dp))
            Text("Coche l'album qui contient tes outfits. Tu pourras en ajouter d'autres plus tard depuis le Menu. Les photos que tu partages depuis Instagram ou Pinterest iront dans l'album « $OWN_ALBUM ».")
        }
        AlbumChooser(onDone = {})
    }
}

@Composable
fun AlbumChooser(onDone: () -> Unit) {
    val ctx = LocalContext.current
    var albums by remember { mutableStateOf<List<Album>?>(null) }
    var picked by remember { mutableStateOf(Store.albums) }
    LaunchedEffect(Unit) {
        albums = withContext(Dispatchers.IO) { runCatching { Media.albums(ctx) }.getOrDefault(emptyList()) }
    }
    val list = albums
    if (list == null) {
        Text("Lecture des albums…", color = FF.Mute)
        return
    }
    if (list.isEmpty()) Text("Aucun album trouvé sur ce téléphone.", color = FF.Mute)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        list.forEach { a ->
            val on = a.id in picked
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                    .background(if (on) FF.Gold.copy(alpha = 0.15f) else Color(0x33000000))
                    .clickable { picked = if (on) picked - a.id else picked + a.id }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(a.cover, null, Modifier.size(54.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.name, fontWeight = FontWeight.Bold)
                    Text("${fmt(a.count)} photos", fontSize = 14.sp, color = FF.Mute)
                }
                if (on) Cursor(14.dp)
            }
        }
    }
    Btn("Valider (${picked.size} album${if (picked.size > 1) "s" else ""})", Modifier.fillMaxWidth(), primary = true, enabled = picked.isNotEmpty()) {
        Store.albums = picked
        Store.save()
        UI.refresh++
        onDone()
    }
}

@Composable
private fun SheetHost(s: Sheet) {
    val close = { UI.sheet = null }
    when (s) {
        Sheet.Menu -> MenuSheet(close)
        Sheet.Albums -> SheetFrame("Albums suivis", close) { AlbumChooser(onDone = close) }
        Sheet.Families -> FamiliesSheet(close)
        is Sheet.TagActions -> TagActionsSheet(s.tag, close)
        is Sheet.BatchAdd -> BatchAddSheet(s.keys, close)
        is Sheet.BatchRemove -> BatchRemoveSheet(s.keys, close)
        is Sheet.EditItem -> EditItemSheet(s, close)
        is Sheet.Notes -> NotesSheet(s.text, close)
        is Sheet.Confirm -> SheetFrame(s.title, close, actions = {
            Btn("Annuler", onClick = close)
            Btn(s.ok, danger = true) { close(); s.action() }
        }) { Text(s.msg) }
    }
}

@Composable
private fun MenuSheet(close: () -> Unit) {
    SheetFrame("Menu", close) {
        MenuEntry("Albums suivis", "${Store.albums.size} album(s) + « $OWN_ALBUM »") { UI.sheet = Sheet.Albums }
        MenuEntry("Familles de tags", Store.fams.joinToString(" · ")) { UI.sheet = Sheet.Families }
        MenuEntry("Sauvegarder", "Exporte tes tags et ta wishlist dans un fichier (à garder sur Drive par exemple)") { close(); UI.doExport() }
        MenuEntry("Restaurer", "Remplace tes données par celles d'une sauvegarde") {
            UI.sheet = Sheet.Confirm("Restaurer ?", "Tes tags et ta wishlist actuels seront remplacés par ceux du fichier choisi.", "Choisir le fichier") { UI.doImport() }
        }
        Text(
            "Astuce : dans la Galerie Samsung, recherche un de tes anciens tags, sélectionne toutes les photos et partage-les vers Fashion Inspo pour leur remettre ce tag d'un coup.",
            fontSize = 14.sp, color = FF.Mute,
        )
    }
}

@Composable
private fun MenuEntry(title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0x33000000)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cursor(10.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title.uppercase(), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(sub, fontSize = 14.sp, color = FF.Mute)
        }
    }
}

@Composable
private fun FamiliesSheet(close: () -> Unit) {
    var name by remember { mutableStateOf("") }
    SheetFrame("Familles de tags", close, actions = { Btn("OK", primary = true, onClick = close) }) {
        Store.fams.forEach { f ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0x33000000)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Orb(famColor(f))
                Spacer(Modifier.width(10.dp))
                Text(f, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text("✕", Modifier.clickable {
                    Store.fams = Store.fams - f
                    Store.famOf = Store.famOf.filterValues { it != f }
                    Store.save()
                }.padding(6.dp), color = FF.Mute)
            }
        }
        FField(name, { name = it }, "Nouvelle famille (ex. Silhouettes)")
        Btn("Ajouter la famille", enabled = name.isNotBlank()) {
            val v = name.trim()
            if (v !in Store.fams) { Store.fams = Store.fams + v; Store.save() }
            name = ""
        }
    }
}

@Composable
private fun BatchAddSheet(keys: List<String>, close: () -> Unit) {
    val quick = remember(Store.tagMap) { (Store.recent + Store.allTags().take(18)).distinct() }
    val apply: (String) -> Unit = { t ->
        Store.addTagTo(keys, t)
        UI.toast = "« $t » ajouté à ${fmt(keys.size)} photos"
        UI.selecting = false; UI.sel = emptySet()
        close()
    }
    SheetFrame("Taguer ${fmt(keys.size)} photos", close) {
        TagInput(onPick = apply)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            quick.forEach { t -> FChip(t, count = Store.count(t)) { apply(t) } }
        }
    }
}

@Composable
private fun BatchRemoveSheet(keys: List<String>, close: () -> Unit) {
    val present = remember(Store.tagMap) { keys.flatMap { Store.tagsOf(it) }.distinct().sortedByDescending { Store.count(it) } }
    SheetFrame("Retirer un tag", close) {
        if (present.isEmpty()) Text("Ces photos n'ont aucun tag.", color = FF.Mute)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            present.forEach { t ->
                FChip(t) {
                    Store.removeTagFrom(keys, t)
                    UI.toast = "« $t » retiré"
                    close()
                }
            }
        }
    }
}

/** Photos reçues par le bouton Partager (Galerie, Instagram, Pinterest…). */
@Composable
private fun ShareInSheet(uris: List<Uri>) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf<List<Triple<Uri, String?, Boolean>>?>(null) }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    val close = { UI.incoming = null }
    LaunchedEffect(uris) {
        info = withContext(Dispatchers.IO) {
            uris.map { u ->
                val ns = Media.nameAndSize(ctx, u)
                if (ns == null) Triple(u, null, false)
                else Triple(u, "${ns.first}~${ns.second}", Media.existsOnDevice(ctx, ns.first, ns.second))
            }
        }
    }
    val r = info
    SheetFrame("Taguer ${fmt(uris.size)} photo${if (uris.size > 1) "s" else ""}", close, actions = {
        Btn("Annuler", onClick = close)
        Btn(if (busy) "En cours…" else "Appliquer", primary = true, enabled = r != null && tags.isNotEmpty() && !busy) {
            busy = true
            scope.launch {
                val keys = withContext(Dispatchers.IO) {
                    r!!.mapNotNull { (u, k, onDevice) -> if (k != null && onDevice) k else Media.saveCopy(ctx, u) }
                }
                tags.forEach { Store.addTagTo(keys, it) }
                UI.refresh++
                UI.toast = "${tags.size} tag(s) appliqué(s) à ${fmt(keys.size)} photo(s)"
                UI.incoming = null
            }
        }
    }) {
        if (r == null) {
            Text("Analyse des photos…", color = FF.Mute)
        } else {
            val onDevice = r.count { it.third }
            val other = r.size - onDevice
            Text(
                buildString {
                    append("${fmt(onDevice)} déjà dans ta galerie")
                    if (other > 0) append(" · ${fmt(other)} seront copiées dans l'album « $OWN_ALBUM »")
                },
                fontSize = 14.sp, color = FF.Mute,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(uris.take(40)) { u ->
                    AsyncImage(u, null, Modifier.width(66.dp).aspectRatio(0.75f).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
                }
            }
        }
        FLabel("Tags à appliquer")
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tags.forEach { t -> FChip(t, on = true, removable = true) { tags = tags - t } }
            }
        }
        val quick = remember(Store.tagMap) { (Store.recent + Store.allTags().take(14)).distinct() }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            quick.filter { it !in tags }.forEach { t -> FChip(t) { tags = tags + t } }
        }
        TagInput(exclude = tags) { tags = tags + it }
    }
}
