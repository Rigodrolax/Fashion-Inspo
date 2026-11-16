package com.zeytekx.fashioninspo.data

data class NoteItem(val cat: String, val name: String, val price: String, val url: String)

private val BULLET = Regex("^\\s*([-*•·▪◦–—]|\\d+[.)]|\\[[ xX]?\\]|☐|☑|✓|✔)\\s*")
private val URL = Regex("https?://\\S+")
private val PRICE = Regex(
    "(\\d[\\d'’ .,]*\\s?(€|eur|euros?|chf|fr\\.?|\\$|£))|((€|chf|\\$|£)\\s?\\d[\\d'’ .,]*)",
    RegexOption.IGNORE_CASE,
)

/**
 * Lit une note de wishlist collée telle quelle.
 * S'il y a des puces : une ligne sans puce = catégorie, une ligne à puce = article.
 * Sans puces : une ligne finissant par « : » = catégorie.
 */
fun parseNotes(txt: String): List<NoteItem> {
    val lines = txt.split(Regex("\\r?\\n")).map { it.replace(' ', ' ') }.filter { it.isNotBlank() }
    val anyBullet = lines.any { BULLET.containsMatchIn(it) }
    val out = mutableListOf<NoteItem>()
    var cat: String? = null
    for (raw in lines) {
        val l = raw.trim()
        val bullet = BULLET.containsMatchIn(raw)
        val header = l.startsWith("#") || (if (anyBullet) !bullet else l.endsWith(":"))
        var t = l.replace(BULLET, "").replace(Regex("^#+\\s*"), "").trim()
        if (header) {
            val c = t.removeSuffix(":").trim()
            if (c.isNotEmpty()) cat = c
            continue
        }
        val url = URL.find(t)?.value ?: ""
        if (url.isNotEmpty()) t = t.replace(url, "").trim()
        val pm = PRICE.find(t)
        val price = pm?.value?.trim() ?: ""
        if (pm != null) t = t.replace(pm.value, "").trim()
        t = t.replace(Regex("[\\s:–—-]+$"), "").trim()
        if (t.isNotEmpty()) out.add(NoteItem(cat ?: "Divers", t, price, url))
    }
    return out
}
