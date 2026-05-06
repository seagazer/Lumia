package com.seagazer.aiimage.data

import android.content.Context
import android.util.Log
import com.seagazer.aiimage.data.local.AppPreferences
import com.seagazer.aiimage.domain.GalleryItem
import com.seagazer.aiimage.util.AppGalleryStorage
import java.io.File
import java.net.URI

class GalleryRepository(private val context: Context) {

    fun loadGallery(): List<GalleryItem> =
        filterReachable(AppPreferences.loadGallery(context))

    fun loadRecycleBin(): List<GalleryItem> =
        filterReachable(AppPreferences.loadRecycleBin(context))

    fun loadPrivateSpace(): List<GalleryItem> =
        filterReachable(AppPreferences.loadPrivateSpace(context))

    fun saveGallery(items: List<GalleryItem>) =
        AppPreferences.saveGallery(context, items)

    fun saveRecycleBin(items: List<GalleryItem>) =
        AppPreferences.saveRecycleBin(context, items)

    fun savePrivateSpace(items: List<GalleryItem>) =
        AppPreferences.savePrivateSpace(context, items)

    fun moveToTrash(item: GalleryItem): GalleryItem =
        moveFile(item, AppGalleryStorage.ensureTrashDirectory(context))

    fun moveToGalleryDir(item: GalleryItem): GalleryItem =
        moveFile(item, AppGalleryStorage.ensureDirectory(context))

    fun moveToPrivateDir(item: GalleryItem): GalleryItem =
        moveFile(item, AppGalleryStorage.ensurePrivateDirectory(context))

    fun deleteFile(item: GalleryItem) {
        if (!item.imageUrl.startsWith("file:")) return
        runCatching {
            val f = File(URI.create(item.imageUrl))
            if (f.isFile) f.delete()
        }
    }

    fun clearAll(): Boolean {
        val galleryDeleted = deleteDirectoryIfExists(AppGalleryStorage.directory(context))
        val cacheDeleted = deleteDirectoryIfExists(File(context.cacheDir, "comfy_out"))
        if (!galleryDeleted || !cacheDeleted) {
            Log.w(TAG, "Failed to clear all local image data. galleryDeleted=$galleryDeleted cacheDeleted=$cacheDeleted")
            return false
        }
        AppPreferences.saveGallery(context, emptyList())
        AppPreferences.saveRecycleBin(context, emptyList())
        AppPreferences.savePrivateSpace(context, emptyList())
        return true
    }

    private fun deleteDirectoryIfExists(dir: File): Boolean {
        return !dir.exists() || dir.deleteRecursively()
    }

    private fun moveFile(item: GalleryItem, destDir: File): GalleryItem {
        if (!item.imageUrl.startsWith("file:")) return item
        val src = runCatching { File(URI.create(item.imageUrl)) }.getOrNull() ?: return item
        if (!src.isFile) return item
        val dest = File(destDir, "${item.id}.png")
        if (src.absolutePath == dest.absolutePath) return item
        if (dest.exists() && !dest.delete()) {
            Log.w(TAG, "Failed to replace existing image: ${dest.absolutePath}")
            return item
        }
        val moved = src.renameTo(dest) || runCatching {
            src.copyTo(dest, overwrite = true)
            if (!dest.isFile || dest.length() <= 0L) return@runCatching false
            if (!src.delete()) Log.w(TAG, "Copied image but failed to delete source: ${src.absolutePath}")
            true
        }.getOrElse {
            Log.w(TAG, "Failed to copy image to ${dest.absolutePath}", it)
            false
        }
        return if (moved && dest.isFile) {
            item.copy(imageUrl = dest.toURI().toString())
        } else {
            Log.w(TAG, "Image move failed, keeping original URI: ${item.id}")
            item
        }
    }

    companion object {
        private const val TAG = "GalleryRepository"
    }

    private fun filterReachable(items: List<GalleryItem>): List<GalleryItem> =
        items.filter { item ->
            when {
                item.imageUrl.startsWith("file:") ->
                    runCatching { File(URI.create(item.imageUrl)).isFile }.getOrDefault(false)
                item.imageUrl.startsWith("http://") || item.imageUrl.startsWith("https://") -> true
                else -> false
            }
        }
}
