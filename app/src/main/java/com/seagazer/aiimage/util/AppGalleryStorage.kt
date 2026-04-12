package com.seagazer.aiimage.util

import android.content.Context
import java.io.File

/**
 * Private app directory for generated PNGs. A [.nomedia] file avoids indexing by gallery apps
 * that scan accessible trees. Export to the device photo library is only via [PublicAlbumSaver].
 */
object AppGalleryStorage {
    fun directory(context: Context): File = File(context.filesDir, "gallery")

    /** Deleted images are moved here until restored or permanently removed. */
    fun trashDirectory(context: Context): File = File(directory(context), "trash")

    /** Private images hidden behind a user password. */
    fun privateDirectory(context: Context): File = File(directory(context), "private")

    fun ensureDirectory(context: Context): File {
        val dir = directory(context)
        dir.mkdirs()
        val noMedia = File(dir, ".nomedia")
        if (!noMedia.exists()) noMedia.createNewFile()
        return dir
    }

    fun ensureTrashDirectory(context: Context): File {
        val dir = trashDirectory(context)
        dir.mkdirs()
        val noMedia = File(dir, ".nomedia")
        if (!noMedia.exists()) noMedia.createNewFile()
        return dir
    }

    fun ensurePrivateDirectory(context: Context): File {
        val dir = privateDirectory(context)
        dir.mkdirs()
        val noMedia = File(dir, ".nomedia")
        if (!noMedia.exists()) noMedia.createNewFile()
        return dir
    }
}
