/*
 * Copyright (c) 2017. The Washington Post. All rights reserved.
 */

package com.wapo.flagship

import com.wapo.android.commons.util.Logger
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.wapo.flagship.features.shared.activities.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Created by elamgodilj on 12/21/17.
 */
@AndroidEntryPoint
open class BaseDebugActivity : BaseActivity() {
    fun showNoResponseToast(message: String = "no response") {
        Logger.d("BaseDebugActivity", "showNoResponseToast $message")
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun showDialog(message: String) {
        Logger.d("BaseDebugActivity", "showDialog $message")
        AlertDialog
            .Builder(this)
            .setMessage(message)
            .create()
            .show()
    }
}
