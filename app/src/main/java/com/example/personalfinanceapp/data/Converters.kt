package com.example.personalfinanceapp.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromFrequency(value: Frequency): String {
        return value.name
    }

    @TypeConverter
    fun toFrequency(value: String): Frequency {
        return Frequency.valueOf(value)
    }
}