// package com.wapo.flagship.features.wear
//
// import android.app.AlertDialog
// import android.app.Dialog
// import android.content.Context
// import android.content.Intent
// import android.net.Uri
// import com.google.android.gms.wearable.Node
// import com.google.android.wearable.intent.RemoteIntent
// import com.washingtonpost.android.R
// import java.util.ArrayList
//
// object WearDialog {
//    private const val PLAY_STORE_APP_URI = "market://details?id=com.washingtonpost.android"
//
//    @JvmStatic
//    fun getDialog(context: Context, message: String, nodes : ArrayList<Node>): Dialog {
//        val builder = AlertDialog.Builder(context)
//        builder.setMessage(message)
//        val yesText = if (nodes.isEmpty()) R.string.rate_app_ok else R.string.install
//        builder.setPositiveButton(yesText) { _, _ ->
//            val intent = Intent(Intent.ACTION_VIEW)
//                    .addCategory(Intent.CATEGORY_BROWSABLE)
//                    .setData(Uri.parse(PLAY_STORE_APP_URI))
//            for (node in nodes) {
//                RemoteIntent.startRemoteActivity(
//                        context,
//                        intent,
//                        null,
//                        node.id)
//            }
//        }
//        if (nodes.isNotEmpty()) {
//            builder.setNegativeButton(R.string.night_mode_dialog_no, null)
//        }
//        builder.setCancelable(true)
//        return builder.create()
//    }
// }
