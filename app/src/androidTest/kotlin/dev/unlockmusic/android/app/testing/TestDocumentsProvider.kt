package dev.unlockmusic.android.app.testing

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.test.platform.app.InstrumentationRegistry

object TestDocuments {
    private val providerUri: Uri =
        Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(TestDocumentsProvider.AUTHORITY)
            .build()

    fun reset(contentResolver: ContentResolver) {
        withManageDocumentsPermission {
            providerOwnerContext().contentResolver.call(providerUri, TestDocumentsProvider.METHOD_RESET, null, null)
        }
    }

    fun seedInputFile(
        contentResolver: ContentResolver,
        displayName: String,
        bytes: ByteArray,
        mimeType: String = "application/octet-stream",
    ): Uri {
        val result =
            requireNotNull(
                withManageDocumentsPermission {
                    providerOwnerContext().contentResolver.call(
                        providerUri,
                        TestDocumentsProvider.METHOD_SEED_FILE,
                        null,
                        Bundle().apply {
                            putString(
                                TestDocumentsProvider.EXTRA_PARENT_DOCUMENT_ID,
                                TestDocumentsProvider.INPUT_DIRECTORY_DOCUMENT_ID,
                            )
                            putString(TestDocumentsProvider.EXTRA_DISPLAY_NAME, displayName)
                            putString(TestDocumentsProvider.EXTRA_MIME_TYPE, mimeType)
                            putByteArray(TestDocumentsProvider.EXTRA_BYTES, bytes)
                        },
                    )
                },
            )

        val documentId =
            requireNotNull(result.getString(TestDocumentsProvider.EXTRA_DOCUMENT_ID)) {
                "Provider did not return a document id"
            }
        return documentUri(documentId).also { uri ->
            grantToTargetApp(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun outputTreeUri(): Uri {
        return DocumentsContract.buildTreeDocumentUri(
            TestDocumentsProvider.AUTHORITY,
            TestDocumentsProvider.OUTPUT_DIRECTORY_DOCUMENT_ID,
        ).also { uri ->
            grantToTargetApp(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
            )
        }
    }

    fun outputDocumentUri(displayName: String): Uri {
        return documentUri("${TestDocumentsProvider.OUTPUT_DIRECTORY_DOCUMENT_ID}/$displayName").also { uri ->
            grantToTargetApp(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun documentUri(documentId: String): Uri {
        return DocumentsContract.buildDocumentUri(
            TestDocumentsProvider.AUTHORITY,
            documentId,
        )
    }

    private fun providerOwnerContext(): Context {
        return InstrumentationRegistry.getInstrumentation().context
    }

    private fun grantToTargetApp(
        uri: Uri,
        flags: Int,
    ) {
        providerOwnerContext().grantUriPermission(
            InstrumentationRegistry.getInstrumentation().targetContext.packageName,
            uri,
            flags,
        )
    }

    private fun <T> withManageDocumentsPermission(block: () -> T): T {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.adoptShellPermissionIdentity(Manifest.permission.MANAGE_DOCUMENTS)
        return try {
            block()
        } finally {
            instrumentation.uiAutomation.dropShellPermissionIdentity()
        }
    }
}
