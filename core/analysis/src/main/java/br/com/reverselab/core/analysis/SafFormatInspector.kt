package br.com.reverselab.core.analysis

import android.content.ContentResolver
import android.net.Uri
import java.io.IOException

/** Reads only a short file header through the granted SAF URI. */
class SafFormatInspector(
    private val contentResolver: ContentResolver,
) {
    fun inspect(uri: Uri, displayName: String): FileDetection {
        val mimeType = contentResolver.getType(uri)
        val header = contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(HEADER_SIZE)
            val count = input.read(buffer)
            if (count <= 0) ByteArray(0) else buffer.copyOf(count)
        } ?: throw IOException("Não foi possível ler o cabeçalho do documento selecionado.")

        return FileTypeDetector.detect(displayName, mimeType, header)
    }

    private companion object { const val HEADER_SIZE = 32 }
}
