package com.java.myapplication.data.local

import androidx.room.TypeConverter
import com.java.myapplication.data.model.*

class Converters {
    @TypeConverter
    fun fromElement(value: Element): String = value.name

    @TypeConverter
    fun toElement(value: String): Element = Element.valueOf(value)

    @TypeConverter
    fun fromPath(value: Path): String = value.name

    @TypeConverter
    fun toPath(value: String): Path = Path.valueOf(value)

    @TypeConverter
    fun fromRole(value: Role): String = value.name

    @TypeConverter
    fun toRole(value: String): Role = Role.valueOf(value)

    @TypeConverter
    fun fromTagSet(value: Set<Tag>): String =
        value.joinToString(",") { it.name }

    @TypeConverter
    fun toTagSet(value: String): Set<Tag> =
        if (value.isEmpty()) emptySet()
        else value.split(",").map { Tag.valueOf(it) }.toSet()

    @TypeConverter
    fun fromStatTypeSet(value: Set<StatType>): String =
        value.joinToString(",") { it.name }

    @TypeConverter
    fun toStatTypeSet(value: String): Set<StatType> =
        if (value.isEmpty()) emptySet()
        else value.split(",").map { StatType.valueOf(it) }.toSet()

    @TypeConverter
    fun fromStringSet(value: Set<String>): String =
        value.joinToString("") { it }

    @TypeConverter
    fun toStringSet(value: String): Set<String> =
        if (value.isEmpty()) emptySet()
        else value.split("").toSet()
}
