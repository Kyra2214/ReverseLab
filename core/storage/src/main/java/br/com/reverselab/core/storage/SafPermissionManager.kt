package br.com.reverselab.core.storage

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri

/** Persists only the narrow read grant returned by the Android Storage Access Framework. */
class SafPermissionManager(
    private val contentResolver: ContentResolver,
) {
    fun persistReadGrant(uri: Uri, resultIntentFlags: Int): Boolean {
        require(resultIntentFlags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0) {
            "O seletor não concedeu permissão de leitura para este documento."
        }

        if (hasReadGrant(uri)) return true

        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        return hasReadGrant(uri)
    }

    fun hasReadGrant(uri: Uri): Boolean = contentResolver.persistedUriPermissions.any {
        it.uri == uri && it.isReadPermission
    }

    /** Releases access only; it never deletes or modifies the referenced document. */
    fun releaseReadGrant(uri: Uri) {
        if (hasReadGrant(uri)) {
            contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
}
