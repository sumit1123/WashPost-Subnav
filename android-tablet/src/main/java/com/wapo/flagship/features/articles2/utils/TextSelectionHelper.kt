package com.wapo.flagship.features.articles2.utils

import android.app.Activity
import android.content.Context
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import com.wapo.flagship.features.articles2.interfaces.TextSelection
import com.wapo.view.selection.SelectableView
import com.wapo.view.selection.SelectionCallback

/**
 * Helper class to that provides components specific to text selection mode in articles.
 */
object TextSelectionHelper {
    /**
     * Returns a [SelectionCallback] instance that takes care of all text selection and actions within text selection mode.
     * [context] context to start action mode.
     * [textSelection] instance to notify the caller about actions within text selection mode.
     */
    fun createSelectionCallback(
        context: Context,
        textSelection: TextSelection,
    ): SelectionCallback {
        return object : SelectionCallback {
            var selectableView: SelectableView? = null
            var mActionMode: ActionMode? = null
            var mActionModeCallback: ActionMode.Callback =
                object : ActionMode.Callback {
                    override fun onCreateActionMode(
                        mode: ActionMode,
                        menu: Menu,
                    ): Boolean {
                        val inflater = mode.menuInflater
                        inflater.inflate(com.washingtonpost.android.articles.R.menu.selection_menu, menu)
                        return true
                    }

                    override fun onPrepareActionMode(
                        mode: ActionMode,
                        menu: Menu,
                    ): Boolean = false

                    override fun onActionItemClicked(
                        mode: ActionMode,
                        item: MenuItem,
                    ): Boolean {
                        val itemId = item.itemId
                        if (itemId == com.washingtonpost.android.articles.R.id.selection_menu_copy) {
                            selectableView?.copyTextToClipboard()
                            mActionMode?.finish()
                            return true
                        }
                        if (itemId == com.washingtonpost.android.articles.R.id.selection_menu_share) {
                            selectableView?.let {
                                textSelection.onSharePreformed(
                                    selectableView?.selectedText,
                                )
                            }
                            mActionMode?.finish()
                            return true
                        }
                        return false
                    }

                    override fun onDestroyActionMode(mode: ActionMode) {
                        selectableView?.resetSelection()
                        mActionMode = null
                    }
                }

            override fun startSelection(selectableView: SelectableView) {
                this.selectableView = selectableView
                if (context is Activity) {
                    mActionMode = context.startActionMode(mActionModeCallback)
                }
            }

            override fun stopSelection(selectableView: SelectableView) {
                mActionMode?.finish()
            }
        }
    }
}
