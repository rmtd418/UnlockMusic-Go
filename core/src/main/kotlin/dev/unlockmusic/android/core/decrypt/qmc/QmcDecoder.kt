package dev.unlockmusic.android.core.decrypt.qmc

class QmcDecoder(
    file: ByteArray,
) {
    private val file = file.copyOf()
    private val size = file.size
    private var decoded = false
    private var audioSize: Int? = null
    private var cipher: QmcStreamCipher? = null

    var songId: Int? = null
        private set

    init {
        searchKey()
    }

    fun decrypt(): ByteArray {
        val currentCipher = requireNotNull(cipher) { "no cipher found" }
        val currentAudioSize = requireNotNull(audioSize) { "invalid audio size" }
        require(currentAudioSize > 0) { "invalid audio size" }

        val audioBuffer = file.copyOfRange(0, currentAudioSize)
        if (!decoded) {
            currentCipher.decrypt(audioBuffer, 0)
            decoded = true
        }
        return audioBuffer
    }

    private fun searchKey() {
        val last4Bytes = file.copyOfRange(size - 4, size)
        when (String(last4Bytes, Charsets.UTF_8)) {
            "STag" -> error("QMC file does not contain an embedded key")
            "QTag" -> parseQTag()
            else -> parseLegacyOrStatic(last4Bytes)
        }
    }

    private fun parseQTag() {
        val keySize = readUInt32BigEndian(file, size - 8)
        audioSize = size - keySize.toInt() - 8

        val rawKey = file.copyOfRange(requireNotNull(audioSize), size - 8)
        val keyEnd = rawKey.indexOf(COMMA_BYTE)
        require(keyEnd >= 0) { "invalid key: search raw key failed" }
        setCipher(rawKey.copyOfRange(0, keyEnd))

        val idBytes = rawKey.copyOfRange(keyEnd + 1, rawKey.size)
        val idEnd = idBytes.indexOf(COMMA_BYTE)
        require(idEnd >= 0) { "invalid key: search song id failed" }
        songId = String(idBytes.copyOfRange(0, idEnd), Charsets.UTF_8).toInt()
    }

    private fun parseLegacyOrStatic(last4Bytes: ByteArray) {
        val keySize = readUInt32LittleEndian(last4Bytes, 0)
        if (keySize < 0x400L) {
            audioSize = size - keySize.toInt() - 4
            val rawKey = file.copyOfRange(requireNotNull(audioSize), size - 4)
            setCipher(rawKey)
        } else {
            audioSize = size
            cipher = QmcStaticCipher()
        }
    }

    private fun setCipher(rawKey: ByteArray) {
        val key = qmcDeriveKey(rawKey)
        cipher =
            if (key.size > 300) {
                QmcRc4Cipher(key)
            } else {
                QmcMapCipher(key)
            }
    }

    private companion object {
        private val COMMA_BYTE = ','.code.toByte()

        private fun readUInt32BigEndian(
            source: ByteArray,
            offset: Int,
        ): Long {
            return ((source[offset].toLong() and 0xFF) shl 24) or
                ((source[offset + 1].toLong() and 0xFF) shl 16) or
                ((source[offset + 2].toLong() and 0xFF) shl 8) or
                (source[offset + 3].toLong() and 0xFF)
        }

        private fun readUInt32LittleEndian(
            source: ByteArray,
            offset: Int,
        ): Long {
            return (source[offset].toLong() and 0xFF) or
                ((source[offset + 1].toLong() and 0xFF) shl 8) or
                ((source[offset + 2].toLong() and 0xFF) shl 16) or
                ((source[offset + 3].toLong() and 0xFF) shl 24)
        }
    }
}
