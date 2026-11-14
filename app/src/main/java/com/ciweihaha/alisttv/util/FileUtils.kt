package com.ciweihaha.alisttv.util

object FileUtils {
    private val VIDEO_EXTS = setOf(
        "mp4", "mkv", "avi", "mov", "webm", "flv", "ts", "m2ts", "rmvb",
        "mpg", "mpeg", "wmv", "3gp", "m4v"
    )
    private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    private val AUDIO_EXTS = setOf("mp3", "flac", "wav", "aac", "ogg", "m4a")

    fun extension(name: String): String =
        name.substringAfterLast('.', "").lowercase()

    fun isVideo(name: String): Boolean = extension(name) in VIDEO_EXTS
    fun isImage(name: String): Boolean = extension(name) in IMAGE_EXTS
    fun isAudio(name: String): Boolean = extension(name) in AUDIO_EXTS

    fun formatSize(size: Long): String {
        if (size < 1024) return "$size B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = size.toDouble()
        var unit = -1
        while (value >= 1024 && unit < units.size - 1) {
            value /= 1024
            unit++
        }
        return String.format("%.1f %s", value, units[unit])
    }

    fun joinPath(parent: String, name: String): String =
        parent.trimEnd('/') + "/" + name

    fun parentPath(path: String): String {
        val p = path.trimEnd('/')
        val idx = p.lastIndexOf('/')
        return if (idx <= 0) "/" else p.substring(0, idx)
    }
}
