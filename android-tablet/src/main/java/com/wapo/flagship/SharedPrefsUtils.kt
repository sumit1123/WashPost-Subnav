package com.wapo.flagship

import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class IntPreference(
    private val preferences: Lazy<SharedPreferences>,
    private val name: String,
    private val defaultValue: Int,
) : ReadWriteProperty<Any, Int> {
    override fun getValue(
        thisRef: Any,
        property: KProperty<*>,
    ): Int = preferences.value.getInt(name, defaultValue)

    override fun setValue(
        thisRef: Any,
        property: KProperty<*>,
        value: Int,
    ) {
        preferences.value
            .edit()
            .putInt(name, value)
            .apply()
    }
}

class LongPreference(
    private val preferences: Lazy<SharedPreferences>,
    private val name: String,
    private val defaultValue: Long,
) : ReadWriteProperty<Any, Long> {
    override fun getValue(
        thisRef: Any,
        property: KProperty<*>,
    ): Long = preferences.value.getLong(name, defaultValue)

    override fun setValue(
        thisRef: Any,
        property: KProperty<*>,
        value: Long,
    ) {
        preferences.value
            .edit()
            .putLong(name, value)
            .apply()
    }
}

class StringPreference(
    private val preferences: Lazy<SharedPreferences>,
    private val name: String,
    private val defaultValue: String,
) : ReadWriteProperty<Any, String> {
    override fun getValue(
        thisRef: Any,
        property: KProperty<*>,
    ): String = preferences.value.getString(name, defaultValue) ?: defaultValue

    override fun setValue(
        thisRef: Any,
        property: KProperty<*>,
        value: String,
    ) {
        preferences.value
            .edit()
            .putString(name, value)
            .apply()
    }
}
