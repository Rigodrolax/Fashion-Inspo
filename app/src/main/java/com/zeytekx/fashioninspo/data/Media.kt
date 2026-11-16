package com.zeytekx.fashioninspo.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns

data class Photo(
    val id: Long,
    val uri: Uri,
    val name: String,
    val size: Long,
    val date: Long,
    val bucket: Long,
) {
    /** Identifiant stable : survit à un changement de téléphone tant que le fichier est identique. */
    val key: String get() = "$name~$size"
}

data class Album(val id: Long, val name: String, val count: Int, val cover: Uri)

const val OWN_ALBUM = "Fashion Inspo"

object Media {
    private val coll: Uri get() = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

    fun albums(ctx: Context): List<Album> {
        val proj = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        )
        data class Acc(val name: String, var count: Int, val cover: Long)
        val map = LinkedHashMap<Long, Acc>()
        ctx.contentResolver.query(coll, proj, null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC")?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val iB = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val iN = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            while (c.moveToNext()) {
                val b = c.getLong(iB)
                val a = map[b]
                if (a == null) map[b] = Acc(c.getString(iN) ?: "Sans nom", 1, c.getLong(iId)) else a.count++
            }
        }
        return map.map { (id, a) -> Album(id, a.name, a.count, ContentUris.withAppendedId(coll, a.cover)) }
            .sortedByDescending { it.count }
    }

    fun photos(ctx: Context, buckets: Set<Long>): List<Photo> {
        val proj = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_ID,
        )
        val sel = StringBuilder("${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?")
        if (buckets.isNotEmpty()) sel.append(" OR ${MediaStore.Images.Media.BUCKET_ID} IN (${buckets.joinToString(",")})")
        val out = ArrayList<Photo>()
        ctx.contentResolver.query(coll, proj, sel.toString(), arrayOf(OWN_ALBUM), null)?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val iN = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val iS = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val iT = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val iA = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val iB = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            while (c.moveToNext()) {
                val id = c.getLong(iId)
                val taken = c.getLong(iT)
                out.add(
                    Photo(
                        id = id,
                        uri = ContentUris.withAppendedId(coll, id),
                        name = c.getString(iN) ?: "photo_$id",
                        size = c.getLong(iS),
                        date = if (taken > 0) taken else c.getLong(iA) * 1000,
                        bucket = c.getLong(iB),
                    )
                )
            }
        }
        out.sortByDescending { it.date }
        return out
    }

    /** Nom et taille d'une image reçue par le bouton Partager. */
    fun nameAndSize(ctx: Context, uri: Uri): Pair<String, Long>? = runCatching {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (!c.moveToFirst()) return@use null
            val n = c.getString(0) ?: return@use null
            if (c.isNull(1)) return@use null
            n to c.getLong(1)
        }
    }.getOrNull()

    fun existsOnDevice(ctx: Context, name: String, size: Long): Boolean = runCatching {
        ctx.contentResolver.query(
            coll, arrayOf(MediaStore.Images.Media._ID),
            "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.SIZE} = ?",
            arrayOf(name, size.toString()), null,
        )?.use { it.count > 0 } ?: false
    }.getOrDefault(false)

    /** Copie une image venue d'une autre app (Instagram, Pinterest…) dans l'album « Fashion Inspo ». */
    fun saveCopy(ctx: Context, src: Uri): String? = runCatching {
        val cr = ctx.contentResolver
        val mime = cr.getType(src) ?: "image/jpeg"
        val ext = when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            else -> "jpg"
        }
        val name = "inspo_${System.currentTimeMillis()}_${(100..999).random()}.$ext"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + OWN_ALBUM)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val dst = cr.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: return null
        var size = 0L
        cr.openInputStream(src)?.use { input ->
            cr.openOutputStream(dst)?.use { output -> size = input.copyTo(output) }
        }
        cr.update(dst, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        if (size == 0L) null else "$name~$size"
    }.getOrNull()
}
