package dev.unlockmusic.android.core.decrypt.kgm

object KgmTestData {
    private const val headerLength = 0x2C
    private val key =
        byteArrayOf(
            0x31,
            0x32,
            0x33,
            0x34,
            0x35,
            0x36,
            0x37,
            0x38,
            0x39,
            0x30,
            0x41,
            0x42,
            0x43,
            0x44,
            0x45,
            0x46,
        )
    private val keyWithTerminator = key + byteArrayOf(0)

    fun kgmFile(audioBytes: ByteArray): ByteArray {
        return encryptedFile(audioBytes, extension = "kgm")
    }

    fun vprFile(audioBytes: ByteArray): ByteArray {
        return encryptedFile(audioBytes, extension = "vpr")
    }

    fun flacAudio(): ByteArray {
        return byteArrayOf(0x66, 0x4C, 0x61, 0x43, 0x00, 0x00, 0x00, 0x22) +
            "synthetic-kgm-flac".toByteArray(Charsets.UTF_8)
    }

    fun mp3Audio(): ByteArray {
        return byteArrayOf(0x49, 0x44, 0x33, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x21) +
            "synthetic-vpr-mp3".toByteArray(Charsets.UTF_8)
    }

    private fun encryptedFile(
        audioBytes: ByteArray,
        extension: String,
    ): ByteArray {
        val mode = if (extension == "vpr") Mode.Vpr else Mode.Kgm
        val header =
            ByteArray(headerLength).apply {
                System.arraycopy(mode.header, 0, this, 0, mode.header.size)
                val headerLengthBytes = littleEndianInt(headerLength)
                System.arraycopy(headerLengthBytes, 0, this, 0x10, headerLengthBytes.size)
                System.arraycopy(key, 0, this, 0x1C, key.size)
            }

        return header + encryptAudio(audioBytes, mode)
    }

    private fun encryptAudio(
        audioBytes: ByteArray,
        mode: Mode,
    ): ByteArray {
        return audioBytes.mapIndexed { index, value ->
            encryptByte(value, index, mode)
        }.toByteArray()
    }

    private fun encryptByte(
        value: Byte,
        offset: Int,
        mode: Mode,
    ): Byte {
        val target = if (mode == Mode.Vpr) value.toInt().and(0xFF) xor vprMaskDiff[offset % vprMaskDiff.size] else value.toInt().and(0xFF)
        var mask = getMask(offset)
        mask = mask xor ((mask and 0x0F) shl 4)
        val medium = target xor mask

        val low = medium and 0x0F
        val high = ((medium ushr 4) and 0x0F) xor low
        val keyMix = (high shl 4) or low

        return (keyWithTerminator[offset % keyWithTerminator.size].toInt().and(0xFF) xor keyMix).toByte()
    }

    private fun getMask(position: Int): Int {
        var offset = position ushr 4
        var value = 0
        while (offset >= 0x11) {
            value = value xor table1[offset % maskTableSize]
            offset = offset ushr 4
            value = value xor table2[offset % maskTableSize]
            offset = offset ushr 4
        }

        return maskV2PreDef[position % maskTableSize] xor value
    }

    private fun littleEndianInt(value: Int): ByteArray {
        return byteArrayOf(
            value.toByte(),
            (value ushr 8).toByte(),
            (value ushr 16).toByte(),
            (value ushr 24).toByte(),
        )
    }

    private enum class Mode(val header: ByteArray) {
        Kgm(KgmDecoder.KGM_HEADER),
        Vpr(KgmDecoder.VPR_HEADER),
        ;
    }

    private const val maskTableSize = 272
    private val vprMaskDiff = KgmDecoder.VPR_MASK_DIFF
    private val table1 = KgmDecoder.TABLE_1
    private val table2 = KgmDecoder.TABLE_2
    private val maskV2PreDef = KgmDecoder.MASK_V2_PRE_DEF
}
