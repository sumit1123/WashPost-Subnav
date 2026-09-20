package com.wapo.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.wapo.view.R

/**
 * Generic dismissible dialog that receives a string and displays it to the user.
 */

class SimpleDialogFragment : DialogFragment() {
    var mText : String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog {
        super.onCreateDialog(savedInstanceState)
        mText = arguments?.getString("text")
        return AlertDialog.Builder(requireContext()).apply {
            setMessage(mText)
                .setNegativeButton(R.string.dismiss_label) { _, _ -> }
        }.create()
    }

    companion object {
        fun newInstance(text: String): SimpleDialogFragment {
            val f = SimpleDialogFragment()

            val args = Bundle()
            args.putString("text", text)
            f.arguments = args
            return f
        }
    }
}