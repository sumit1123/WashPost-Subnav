package com.wapo.flagship.features.settings.preferences

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

class ButtonPreference : Preference {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    private val accessibilityDelegate: AccessibilityDelegateCompat =
        object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                nodeInfo: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, nodeInfo)
                nodeInfo.className = Button::class.java.name
            }
        }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.post {
            ViewCompat.setAccessibilityDelegate(
                holder.itemView,
                accessibilityDelegate
            )
        }
    }
}