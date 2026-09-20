package com.wapo.flagship.content.notifications

import android.content.ContentValues
import android.database.Cursor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wapo.flagship.data.ITableDescription

class NotificationTable {
    fun createContentValues(notification: NotificationData): ContentValues {
        val cv = ContentValues()
        if (notification.id != -1) {
            cv.put(IdColumn, notification.id)
        }
        notification.notifId?.let {
            if (it.isNotEmpty()) {
                cv.put(NotifIdColumn, Integer.valueOf(it))
            }
        }

        cv.put(DataColumn, serializeNotification(notification))
        return cv
    }

    private fun serializeNotification(notification: NotificationData): String {
        val jsonNotificationString = Gson().toJson(notification)
        return jsonNotificationString
    }

    companion object : ITableDescription {
        override fun getKeys(): Array<String> = emptyArray()

        @JvmField val Name: String = "NotificationTable"

        @JvmField val IdColumn = "id"

        @JvmField val NotifIdColumn = "notifId"

        @JvmField val DataColumn = "Data"

        @JvmField val ColumnNames = arrayOf(IdColumn, NotifIdColumn, DataColumn)

        val ColumnTypes = arrayOf("INTEGER PRIMARY KEY AUTOINCREMENT", "INTEGER", "TEXT")
        private val NO_SQL = emptyArray<String>()

        override fun getTableName() = Name

        override fun getColumns(): Array<out String>? = ColumnNames

        override fun getColumnsTypes(): Array<out String>? = ColumnTypes

        override fun getPostCreationSql(): Array<String> = NO_SQL

        override fun getPreDeletionSql(): Array<String> = NO_SQL

        @JvmStatic
        fun create(cursor: Cursor): NotificationData? {
            var deserializeNotification: NotificationData? = null
            try {
                deserializeNotification = deserializeNotification(cursor)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return deserializeNotification
        }

        private fun deserializeNotification(cursor: Cursor): NotificationData {
            val id = cursor.getInt(ColumnNames.indexOf(IdColumn))
            val string = cursor.getString(ColumnNames.indexOf(DataColumn))
            val notification =
                Gson().fromJson<NotificationData>(
                    string,
                    object : TypeToken<NotificationData>() {}.type,
                )
            notification.id = id
            return notification
        }

        @JvmStatic
        fun getTableDescription(): ITableDescription = this
    }
}
