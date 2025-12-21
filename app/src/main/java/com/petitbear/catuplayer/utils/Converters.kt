// utils/Converters.kt
package com.petitbear.catuplayer.utils

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object Converters {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun stringListToString(value: List<String>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun stringToStringList(value: String): List<String> {
        return try {
            json.decodeFromString<List<String>>(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun stringToMap(value: String): Map<String, String> {
        return try {
            json.decodeFromString<Map<String, String>>(value)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun mapToString(value: Map<String, String>): String {
        return json.encodeToString(value)
    }
}