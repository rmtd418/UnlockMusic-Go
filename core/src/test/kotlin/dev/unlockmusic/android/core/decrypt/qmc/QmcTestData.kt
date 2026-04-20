package dev.unlockmusic.android.core.decrypt.qmc

object QmcTestData {
    fun bytes(name: String): ByteArray {
        return checkNotNull(javaClass.classLoader.getResourceAsStream("testdata/$name")) {
            "Missing test resource: $name"
        }.readBytes()
    }
}

