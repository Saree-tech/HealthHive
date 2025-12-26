package com.example.healthhive.data.local

import androidx.room.TypeConverter
import com.example.healthhive.ui.screens.RecurrenceType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromRecurrence(value: RecurrenceType): String = value.name

    @TypeConverter
    fun toRecurrence(value: String): RecurrenceType = RecurrenceType.valueOf(value)
}