package com.petitbear.catuplayer.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.annotation.RawRes
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

object FileReadingUtils {
    fun readFileFromAssets(context: Context, filename: String): String {
        return try {
            context.assets.open(filename).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            "Error reading file:${e.message}"
        }
    }

    fun readFileFromRaw(context: Context, @RawRes resId: Int): String {
        return try {
            context.resources.openRawResource(resId).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            "Error reading file: ${e.message}"
        }
    }

    // 写入文件到内部存储
    fun writeToInternalStorage(context: Context, filename: String, content: String) {
        try {
            context.openFileOutput(filename, Context.MODE_PRIVATE).use {
                it.write(content.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 从内部存储读取文件
    fun readFromInternalStorage(context: Context, filename: String): String {
        return try {
            context.openFileInput(filename).bufferedReader().use { it.readText() }
        } catch (e: FileNotFoundException) {
            "File not found"
        } catch (e: IOException) {
            "Error reading file: ${e.message}"
        }
    }

    // 检查外部存储是否可用
    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    // 从外部存储读取文件
    fun readFileFromExternalStorage(filename: String): String {
        if (!isExternalStorageWritable()) {
            return "External storage not available"
        }

        return try {
            val file = File(Environment.getExternalStorageDirectory(), filename)
            file.bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            "Error reading file: ${e.message}"
        }
    }

    fun readFileFromContentUri(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: "Error: Could not open stream from URI"
        } catch (e: IOException) {
            "Error reading file from URI: ${e.message}"
        } catch (e: SecurityException) {
            "Security exception: No permission to read this URI: ${e.message}"
        }
    }
}