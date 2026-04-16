package com.mtc.termosonda.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.IOException
import javax.inject.Inject
import java.io.FileOutputStream


class FileRepo @Inject constructor(private val context: Context) {

    fun createFileAndWrite(fileName: String, content: String): File? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val mediaFile = saveToDownloadsMediaStore(fileName, content)
                if (mediaFile != null) return mediaFile
            }
            // Fallback: internal o legacy
            saveToInternal(fileName, content)
        } catch (e: Exception) {
            Log.e("FileRepo", "Error: ${e.message}")
            null
        }
    }

    private fun saveToDownloadsMediaStore(fileName: String, content: String): File? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, true)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        if (uri == null) {
            Log.w("FileRepo", "MediaStore insert null - storage issue?")
            return null
        }

        try {
            resolver.openOutputStream(uri)?.use { os ->
                os.write(content.toByteArray(Charsets.UTF_8))
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, false)
            resolver.update(uri, values, null, null)

            // Retorna File path para compatibilidad
            return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        } catch (e: IOException) {
            resolver.delete(uri, null, null) // Limpia si falla
            return null
        }
    }

    private fun saveToInternal(fileName: String, content: String): File {
        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use {
            it.write(content.toByteArray(Charsets.UTF_8))
        }
        return file
    }
}