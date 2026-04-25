package dev.unlockmusic.android.core.decrypt

import dev.unlockmusic.android.core.decrypt.kgm.KgmFileDecryptor
import dev.unlockmusic.android.core.decrypt.ncm.NcmFileDecryptor
import dev.unlockmusic.android.core.decrypt.qmc.QmcFileDecryptor

object DefaultFileDispatchDecryptor {
    fun create(): FileDispatchDecryptor {
        return FileDispatchDecryptor(
            decryptors =
                listOf(
                    QmcFileDecryptor(),
                    NcmFileDecryptor(),
                    KgmFileDecryptor(),
                ),
        )
    }
}
