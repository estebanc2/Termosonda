package com.mtc.termosonda.data

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

class FileRepo @Inject constructor(private val context: Context) {
    fun createFileAndWrite(fileName: String, content: String): File? {
        return try {
            val file = File(context.filesDir, fileName)
            FileOutputStream(file).use {
                it.write(content.toByteArray())
            }
            file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
