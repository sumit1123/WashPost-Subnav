/*
 * Copyright (c) 2017. The Washington Post. All rights reserved.
 */

package com.wapo.flagship

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import com.washingtonpost.android.paywall.PaywallService
import dagger.hilt.android.AndroidEntryPoint

/**
 * Created by elamgodilj on 12/21/17.
 */
@AndroidEntryPoint
class DatabaseDebugActivity :
    BaseDebugActivity(),
    View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var wrapContentLayoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        var layout = LinearLayout(this)
        layout.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
            )
        layout.orientation = LinearLayout.VERTICAL

        var showPaywallTables = Button(this)
        showPaywallTables.layoutParams = wrapContentLayoutParams
        showPaywallTables.text = "Show Paywall Tables"
        showPaywallTables.tag = "Show Paywall Tables"
        showPaywallTables.setOnClickListener(this)
        layout.addView(showPaywallTables)

        setContentView(layout)
    }

    override fun onClick(v: View?) {
        when (v?.tag) {
            "Show Paywall Tables" -> {
                val paywallDB = PaywallService.getConnector().db
                val cursor =
                    paywallDB.rawQuery(
                        "SELECT name FROM sqlite_master WHERE type='table'",
                        null,
                    )
                cursor.moveToFirst()
                val sb = StringBuilder()
                while (cursor.moveToNext()) {
                    val tableName = cursor.getString(cursor.getColumnIndex("name"))
                    if (tableName != null) {
                        sb.append("$tableName =>> ")

                        val tableCursor = paywallDB.rawQuery("SELECT * FROM $tableName", null)
                        tableCursor.columnNames.forEach {
                            sb.append("$it,")
                        }
                        tableCursor.close()

                        sb.append("\n\n\n")
                    }
                }
                cursor.close()
                showDialog(sb.toString())
            }
        }
    }
}
