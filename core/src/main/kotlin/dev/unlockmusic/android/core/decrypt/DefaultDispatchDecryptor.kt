package dev.unlockmusic.android.core.decrypt

import dev.unlockmusic.android.core.decrypt.kgm.KgmDecryptor
import dev.unlockmusic.android.core.decrypt.ncm.NcmDecryptor
import dev.unlockmusic.android.core.decrypt.qmc.QmcDecryptor

object DefaultDispatchDecryptor {
    fun create(): DispatchDecryptor {
        return DispatchDecryptor(
            decryptors =
                listOf(
                    QmcDecryptor(),
                    NcmDecryptor(),
                    KgmDecryptor(),
                ),
        )
    }
}

