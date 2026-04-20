package dev.unlockmusic.android.core.metadata

data class ParsedFilename(
    val originalName: String,
    val baseName: String,
    val extension: String,
)

object FilenameParser {
    fun parse(filename: String): ParsedFilename {
        val trimmed = filename.trim()
        val separator = trimmed.lastIndexOf('.')
        if (separator <= 0 || separator == trimmed.length - 1) {
            return ParsedFilename(
                originalName = trimmed,
                baseName = trimmed,
                extension = "",
            )
        }

        return ParsedFilename(
            originalName = trimmed,
            baseName = trimmed.substring(0, separator),
            extension = trimmed.substring(separator + 1).lowercase(),
        )
    }
}

fun detectFileType(filename: String): DetectedFileType {
    val extension = FilenameParser.parse(filename).extension
    return DetectedFileType.entries.firstOrNull { extension in it.extensions } ?: DetectedFileType.UNKNOWN
}

