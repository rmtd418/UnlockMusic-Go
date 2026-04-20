package dev.unlockmusic.android.core.metadata

object AudioHeaderSniffer {
    private val flacHeader = byteArrayOf(0x66, 0x4C, 0x61, 0x43)
    private val mp3Header = byteArrayOf(0x49, 0x44, 0x33)
    private val oggHeader = byteArrayOf(0x4F, 0x67, 0x67, 0x53)
    private val m4aHeader = byteArrayOf(0x66, 0x74, 0x79, 0x70)
    private val wavHeader = byteArrayOf(0x52, 0x49, 0x46, 0x46)
    private val wmaHeader =
        byteArrayOf(
            0x30,
            0x26,
            0xB2.toByte(),
            0x75,
            0x8E.toByte(),
            0x66,
            0xCF.toByte(),
            0x11,
            0xA6.toByte(),
            0xD9.toByte(),
            0x00,
            0xAA.toByte(),
            0x00,
            0x62,
            0xCE.toByte(),
            0x6C,
        )
    private val aacHeader = byteArrayOf(0xFF.toByte(), 0xF1.toByte())
    private val dffHeader = byteArrayOf(0x46, 0x52, 0x4D, 0x38)

    fun sniffExtension(
        data: ByteArray,
        fallbackExtension: String = "mp3",
    ): String {
        if (hasPrefix(data, mp3Header)) return "mp3"
        if (hasPrefix(data, flacHeader)) return "flac"
        if (hasPrefix(data, oggHeader)) return "ogg"
        if (data.size >= 8 && hasPrefix(data.copyOfRange(4, 8), m4aHeader)) return "m4a"
        if (hasPrefix(data, wavHeader)) return "wav"
        if (hasPrefix(data, wmaHeader)) return "wma"
        if (hasPrefix(data, aacHeader)) return "aac"
        if (hasPrefix(data, dffHeader)) return "dff"
        return fallbackExtension
    }

    private fun hasPrefix(
        data: ByteArray,
        prefix: ByteArray,
    ): Boolean {
        if (prefix.size > data.size) return false
        return prefix.indices.all { index -> data[index] == prefix[index] }
    }
}

