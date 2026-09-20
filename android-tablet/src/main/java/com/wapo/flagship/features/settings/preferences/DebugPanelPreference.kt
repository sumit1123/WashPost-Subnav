package com.wapo.flagship.features.settings.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.wapo.flagship.apienvironment.APIEnvironmentsExpandableListAdapter
import com.wapo.flagship.features.settings.AppPreferences
import com.wapo.flagship.util.PrefUtils
import com.washingtonpost.android.BuildConfig
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.ConfigOverride

class DebugPanelPreference : Preference {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    private var adapter: APIEnvironmentsExpandableListAdapter? = null

    private var expListView: NonScrollableExpandableListView? = null
    var paywallClick: (() -> Unit)? = null

    init {
        layoutResource = R.layout.preference_debug_panel_layout
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        expListView = holder.findViewById(R.id.debugPanelExpandableList) as? NonScrollableExpandableListView
        setupExpandableList()
    }

    private fun setupExpandableList() {
        val listView = expListView ?: return
        val ctx = context

        val expandableListItems = getListViewData()
        val expandableListHeaders = ArrayList<String>(expandableListItems.keys)

        adapter = APIEnvironmentsExpandableListAdapter(
            ctx,
            expandableListHeaders,
            expandableListItems
        ).also {
            it.closeDialog = { saveSelectedOverrides() }
        }

        listView.setOnGroupClickListener { _, _, groupPosition, _ ->
            val title = expandableListHeaders[groupPosition]
            when (title) {
                PAYWALL_TITLE -> {
                    paywallClick?.invoke()
                    true
                }
                FIREBASE_DEBUGGER_TITLE -> {

                    val enableCmd = "adb shell setprop debug.firebase.analytics.app ${context.packageName} && adb shell am force-stop ${context.packageName} && adb shell am start -n ${context.packageName}/com.wapo.flagship.wapomain.MainActivity"
                    val disableCmd = "adb shell setprop debug.firebase.analytics.app .none. && adb shell am force-stop ${context.packageName} && adb shell am start -n ${context.packageName}/com.wapo.flagship.wapomain.MainActivity"

                    val message = "Run this command in terminal to enable Firebase DebugView:\n\n$enableCmd\n\nTo disable:\n\n$disableCmd"
                    android.app.AlertDialog.Builder(context)
                        .setTitle("Firebase Debugger")
                        .setMessage(message)
                        .setNeutralButton("Copy Enable") { _, _ ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                    as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("adb", enableCmd))
                            android.widget.Toast.makeText(context, "Copied", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Copy Disable") { _, _ ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                    as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("adb", disableCmd))
                            android.widget.Toast.makeText(context, "Copied", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        .setPositiveButton("OK", null)
                        .show()
                    true
                }
                else -> false
            }
        }

        listView.setAdapter(adapter)

        // Remove the default system indicator - we manage it ourselves in the adapter
        listView.setGroupIndicator(null)

        // Set indicator bounds
        val displayMetrics = ctx.resources.displayMetrics
        val indicatorSize = (16 * displayMetrics.density).toInt()
        val indicatorPadding = (8 * displayMetrics.density).toInt()
        listView.setIndicatorBounds(
            listView.paddingLeft + indicatorPadding,
            listView.paddingLeft + indicatorPadding + indicatorSize
        )

        // Auto-expand groups based on active state
        expandGroupsWithActiveSettings(listView, expandableListHeaders)

        // Handle group expand - prevent empty groups and recalculate height
        listView.setOnGroupExpandListener { groupPosition ->
            if (adapter?.getChildrenCount(groupPosition) == 0) {
                listView.collapseGroup(groupPosition)
            } else {
                // Recalculate height after expand
                listView.post {
                    listView.requestLayout()
                }
            }
        }

        // Handle group collapse - recalculate height
        listView.setOnGroupCollapseListener {
            listView.post {
                listView.requestLayout()
            }
        }
    }

    private fun expandGroupsWithActiveSettings(
        expListView: NonScrollableExpandableListView,
        headers: List<String>
    ) {
        // Expand Paywall if any paywall checkboxes are active
        val hasPaywallActive = PrefUtils.getPrefPaywallHide(context) || PrefUtils.getVerifyOnEachLaunch(context)
        if (hasPaywallActive) {
            headers.indexOf(PAYWALL_TITLE).takeIf { it >= 0 }?.let {
                expListView.expandGroup(it)
            }
        }

        // Expand LeakCanary if dump heap is active
        if (BuildConfig.DEBUG && PrefUtils.getPrefLeakCanaryDumpHeap(context)) {
            headers.indexOf(LEAK_CANARY_TITLE).takeIf { it >= 0 }?.let {
                expListView.expandGroup(it)
            }
        }

        if (PrefUtils.getPrefOneTrustStage(context)) {
            headers.indexOf(ONETRUST_TITLE).takeIf { it >= 0 }?.let {
                expListView.expandGroup(it)
            }
        }

        if (BuildConfig.DEBUG && PrefUtils.getVerifyOnEachLaunch(context)) {
            headers.indexOf(VERIFY_ON_LAUNCH_TITLE).takeIf { it >= 0 }?.let {
                expListView.expandGroup(it)
            }
        }
    }

    private fun getListViewData(): HashMap<String, List<String>> {
        return LinkedHashMap<String, List<String>>().apply {
            put(
                CONFIG_ENVIRONMENT_TITLE,
                ConfigOverride.entries
                    .filter { it.isVisible }
                    .sortedBy { it.name }
                    .map { CONFIG_ENVIRONMENT_ITEM_PREFIX + it.name }
            )
            put(SHORT_AB_TESTS, emptyList())
            put(LONG_AB_TESTS, emptyList())
            put(FIREBASE_DEBUGGER_TITLE, emptyList())
            put(DEEP_LINK_TITLE, listOf(DEEP_LINK_PANEL))
            put(ONETRUST_TITLE, listOf(ONETRUST_PANEL))
            put(BLANK_CHECKBOX_TITLE, listOf(BLANK_CHECKBOX_PANEL))
            put(PAYWALL_TITLE, emptyList())
            if (BuildConfig.DEBUG) put(LEAK_CANARY_TITLE, listOf(DUMP_HEAP_PANEL))
            if (BuildConfig.DEBUG) {
                put(
                    AGE_RESTRICTION_TITLE,
                    listOf(AGE_RESTRICTION_V004_PANEL)
                )
            }
            if (BuildConfig.DEBUG) put(VERIFY_ON_LAUNCH_TITLE, listOf(VERIFY_ON_LAUNCH_PANEL))
        }
    }

    fun saveSelectedOverrides() {
        adapter?.let { adapter ->
            val selectedOverrides = adapter.selectedConfigOverrides.toList()
            ConfigManager.getInstance().setOverrides(selectedOverrides)
            // Persist beta webview preference for non-blocking reads
            AppPreferences.setBetaWebviewOverrideEnabled(selectedOverrides.contains(ConfigOverride.WEBVIEW_BETA))
        }
    }

    companion object {
        const val FUSION_PROD = "fusion_prod"
        const val PROD = "prod"

        const val CONFIG_ENVIRONMENT_TITLE = "🌍 Configure Environment"
        const val CONFIG_ENVIRONMENT_ITEM_PREFIX = "OVERRIDE/"

        const val PAYWALL_TITLE = "💸 Paywall Debugger"
        const val FIREBASE_DEBUGGER_TITLE = "🧫 Firebase Debugger"


        const val DEEP_LINK_TITLE = "🔗 Open Deep Link"
        const val DEEP_LINK_PANEL = "$DEEP_LINK_TITLE Panel"

        const val ONETRUST_TITLE = "🛡️ OneTrust Environment"
        const val ONETRUST_PANEL = "$ONETRUST_TITLE Panel"
        const val BLANK_CHECKBOX_TITLE = "💸 Bypass Paywall Checkbox"
        const val BLANK_CHECKBOX_PANEL = "BLANK_CHECKBOX_PANEL"

        const val SHORT_AB_TESTS = "🧪 Short A/B Tests"
        const val LONG_AB_TESTS  = "🔬 Long A/B Tests"


        const val LEAK_CANARY_TITLE = "🐤 LeakCanary"
        const val DUMP_HEAP_PANEL = "Dump heap"

        const val AGE_RESTRICTION_TITLE = "🚫 Age Restrictions"
        const val AGE_RESTRICTION_V004_PANEL = "Age Signals 0.0.4"

        const val VERIFY_ON_LAUNCH_TITLE = "🔁 Verify User On Each Launch"
        const val VERIFY_ON_LAUNCH_PANEL = "VERIFY_ON_LAUNCH_PANEL"

    }
}
