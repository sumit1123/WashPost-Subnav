package com.wapo.android.commons.util

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import com.wapo.android.commons.util.agerestriction.amazon.AmazonUserDataClient
import com.wapo.android.commons.util.agerestriction.amazon.model.UserAgeDataResponse

class TestContentProvider: ContentProvider() {

    private val sUriMatcher: UriMatcher = UriMatcher(UriMatcher.NO_MATCH)

    override fun onCreate(): Boolean {
        sUriMatcher.addURI(AmazonUserDataClient.AUTHORITY, "getUserAgeData", 1);
        return true;
    }
    override fun getType(p0: Uri): String? {
        return null;
    }

    override fun insert(p0: Uri, p1: ContentValues?): Uri? {
        throw UnsupportedOperationException("Insert not supported");
    }

    override fun delete(p0: Uri, p1: String?, p2: Array<out String?>?): Int {
        throw UnsupportedOperationException("Delete not supported");
    }

    override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String?>?): Int {
        throw UnsupportedOperationException("Update not supported");
    }

    override fun query(uri: Uri, projection: Array<String?>?, queryArgs: Bundle?, cancellationSignal: CancellationSignal?
    ): Cursor? {
        return query(uri, null, null, null, null, cancellationSignal)
    }

    override fun query(uri: Uri, projection: Array<String?>?, selection: String?, selectionArgs: Array<String?>?, sortOrder: String?): Cursor? {
        return query(uri, projection, selection, selectionArgs, sortOrder, null)
    }

    override fun query(uri: Uri, projection: Array<String?>?, selection: String?, selectionArgs: Array<String?>?, sortOrder: String?, cancellationSignal: CancellationSignal?): Cursor {
        val match: Int = sUriMatcher.match(uri)
        require(match == 1) { "Unknown URI: " + uri }

        val param1 = uri.getQueryParameter("testOption")
        requireNotNull(param1) { "testOption parameter is required" }

        val option: Int
        try {
            option = param1.toInt()
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("testOption must be a valid integer")
        }

        require(!(option < 1 || option > 11)) { "testOption must be between 1 and 11" }
        val cursor: MatrixCursor = createCursor()
        cursor.addRow(getResponse(option))
        return cursor
    }

    private fun getResponse(option: Int): Array<Any?> {
        when (option) {
            1 -> return getResponse1()
            2 -> return getResponse2()
            3 -> return getResponse3()
            4 -> return getResponse4()
            5 -> return getResponse5()
            6 -> return getResponse6()
            7 -> return getResponse7()
            8 -> return getResponse8()
            9 -> return getResponse9()
            10 -> return getResponse10()
            11 -> return getResponse11()
        }
        return getResponse1()
    }

    private fun createCursor(): MatrixCursor {
        return MatrixCursor(
            arrayOf<String?>(
                UserAgeDataResponse.COLUMN_RESPONSE_STATUS,
                UserAgeDataResponse.COLUMN_USER_STATUS,
                UserAgeDataResponse.COLUMN_AGE_LOWER,
                UserAgeDataResponse.COLUMN_AGE_UPPER,
                UserAgeDataResponse.COLUMN_USER_ID,
                UserAgeDataResponse.COLUMN_MOST_RECENT_APPROVAL_DATE
            )
        )
    }

    /***
     * Sample response for 18+ user who is age verified
     * For more details see API documentation.
     */
    private fun getResponse1(): Array<Any?> {
        return arrayOf<Any?>("SUCCESS", "VERIFIED", 18, null, null, null)
    }


    /***
     * Sample response for any user where age verification/consent is not ascertainable.
     * For more details see API documentation.
     */
    private fun getResponse2(): Array<Any?> {
        return arrayOf<Any?>("SUCCESS", "UNKNOWN", null, null, null, null)
    }


    /***
     * Response for user with age range between 0 to 12.
     * For more details see API documentation.
     */
    private fun getResponse3(): Array<Any?> {
        return arrayOf<Any>(
            "SUCCESS",
            "SUPERVISED",
            0,
            12,
            "randomTestUserId1jC3QQeivdAytchaIVkWOjDiDlvz8xglhUcLwkbUHNQZKKw",
            "2023-07-01T00:00:00.008+02:00"
        ) as Array<Any?>
    }


    /***
     * Response for user with age range between 13 to 15.
     * For more details see API documentation.
     */
    private fun getResponse4(): Array<Any?> {
        return arrayOf<Any>(
            "SUCCESS",
            "UNKNOWN",
            13,
            15,
            "randomTestUserId1jC3QQeivdAytchaIVkWOjDiDlvz8pUxhUcLwkbUHNQZKKw",
            "2023-07-01T00:00:00.008Z"
        ) as Array<Any?>
    }


    /***
     * Response for user with age range between 16 to 17.
     * For more details see API documentation.
     */
    private fun getResponse5(): Array<Any?> {
        return arrayOf<Any>(
            "SUCCESS",
            "SUPERVISED",
            16,
            17,
            "randomTestUserId1jC3QQeivdhBcchaIVkWOjDiDlvz8pUxhUcLwkbUHNQZKKw",
            "2023-07-01T00:00:00.008Z"
        ) as Array<Any?>
    }

    /*
      * Sample response for a user under 18 years old where consent is pending or not granted.
      * The following code uses "0, 12" as the age interval, but you can change this to suit your test case. Valid age intervals are "0, 12", "13, 15", and "16, 17".
      * For more details see API documentation.
     */
    private fun getResponse6(): Array<Any?> {
        return arrayOf<Any>(
            "SUCCESS",
            "CONSENT_NOT_GRANTED",
            0,
            12,
            "randomTestUserId1jC3QQeivdAytchaIVkWOjDiDlvz8xglhUcLwkbUHNQZKKw",
            "2023-07-01T00:00:00.008+02:00"
        ) as Array<Any?>
    }


    /***
     * Response for any scenarios in general where age verification law is not applicable.
     * For more details see API documentation.
     */
    private fun getResponse7(): Array<Any?> {
        return arrayOf<Any?>("SUCCESS", "", null, null, null, null)
    }


    /***
     * Response when API returns APP_NOT_OWNED status
     * For more details see API documentation.
     */
    private fun getResponse8(): Array<Any?> {
        return arrayOf<Any?>("APP_NOT_OWNED", "", null, null, null, null)
    }


    /***
     * Response when API returns INTERNAL_TRANSIENT_ERROR status
     * For more details see API documentation.
     */
    private fun getResponse9(): Array<Any?> {
        return arrayOf<Any?>("INTERNAL_TRANSIENT_ERROR", "", null, null, null, null)
    }


    /***
     * Response when API returns INTERNAL_ERROR status
     * For more details see API documentation.
     */
    private fun getResponse10(): Array<Any?> {
        return arrayOf<Any?>("INTERNAL_ERROR", "", null, null, null, null)
    }


    /***
     * Response when API returns FEATURE_NOT_SUPPORTED status
     * For more details see API documentation.
     */
    private fun getResponse11(): Array<Any?> {
        return arrayOf<Any?>("FEATURE_NOT_SUPPORTED", "", null, null, null, null)
    }
}
