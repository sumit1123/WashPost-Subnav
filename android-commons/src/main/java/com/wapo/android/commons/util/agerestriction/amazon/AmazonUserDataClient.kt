package com.wapo.android.commons.util.agerestriction.amazon

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.CancellationSignal
import androidx.core.net.toUri
import com.wapo.android.commons.util.agerestriction.amazon.model.UserData
import com.wapo.android.commons.util.agerestriction.amazon.model.UserAgeDataResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class AmazonUserDataClient(context: Context) {

    private var contentResolver: ContentResolver? = context.contentResolver

    suspend fun getMyData(): UserData? {
        val signal = CancellationSignal()
        val future: Deferred<UserData?> = CoroutineScope(Dispatchers.IO).async {
            queryUserDataInternal(signal)
        }

        return future.await()
    }

    private fun queryUserDataInternal(signal: CancellationSignal?): UserData? {
        return try {
            val cursor = contentResolver?.query(getContentUri(), null, null, null, null, signal)

            if (cursor != null) {
                cursor.moveToFirst()

                val ageLowerColumnIndex = cursor.getColumnIndex(UserAgeDataResponse.COLUMN_AGE_LOWER)
                val ageUpperColumnIndex = cursor.getColumnIndex(UserAgeDataResponse.COLUMN_AGE_UPPER)

                val userData = UserData(
                    responseStatus = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            UserAgeDataResponse.COLUMN_RESPONSE_STATUS
                        )
                    ),
                    userStatus = cursor.getString(cursor.getColumnIndexOrThrow(UserAgeDataResponse.COLUMN_USER_STATUS)),
                    ageLower = if (!cursor.isNull(ageLowerColumnIndex)) cursor.getInt(
                        ageLowerColumnIndex
                    ) else null,
                    ageUpper = if (!cursor.isNull(ageUpperColumnIndex)) cursor.getInt(
                        ageUpperColumnIndex
                    ) else null,
                    userId = cursor.getString(cursor.getColumnIndexOrThrow(UserAgeDataResponse.COLUMN_USER_ID)),
                    mostRecentApprovalDate = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            UserAgeDataResponse.COLUMN_MOST_RECENT_APPROVAL_DATE
                        )
                    ),
                )
                cursor.close()
                userData
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun clean() {
        contentResolver = null
    }

    companion object {
        // This endpoint specified below is purely for testing purposes.
        // Replace with "amzn_appstore" for production/live API.
        // Replace with "amzn_test_appstore" tests.
        const val AUTHORITY: String = "amzn_appstore"

        // Replace with /getUserAgeData?testOption=1 for test purposes
        // Modify the query parameter testOption with a value from 1 to 11 (for example, testOption=1 to testOption=11)
        // to get corresponding getResponse1() to getResponse11() output of TestContentProvider.
        // Replace with "/getUserAgeData" for production/live API.
        private const val PATH = "/getUserAgeData"

        fun getContentUri(): Uri {
            return ("content://$AUTHORITY$PATH").toUri()
        }
    }
}