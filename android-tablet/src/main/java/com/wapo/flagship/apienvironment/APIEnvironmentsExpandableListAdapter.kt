package com.wapo.flagship.apienvironment

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseExpandableListAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.wapo.android.commons.util.ViewUtil.findComponentActivity
import com.wapo.android.commons.util.agerestriction.fakeagerestrictionshelper.AgeRestrictionsFakeSetUp
import com.wapo.android.commons.util.agerestriction.fakeagerestrictionshelper.AgeSignalsV004AccessStatuses
import com.wapo.android.commons.util.agerestriction.fakeagerestrictionshelper.AgeSignalsV004SignificantChangeStatuses
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.features.onetrust.OneTrustHelper
import com.wapo.flagship.features.settings.preferences.DebugPanelPreference.Companion.AGE_RESTRICTION_V004_PANEL
import com.wapo.flagship.features.settings.preferences.DebugPanelPreference.Companion.BLANK_CHECKBOX_PANEL
import com.wapo.flagship.features.settings.preferences.DebugPanelPreference.Companion.CONFIG_ENVIRONMENT_ITEM_PREFIX
import com.wapo.flagship.features.settings.preferences.DebugPanelPreference.Companion.DEEP_LINK_PANEL
import com.wapo.flagship.features.settings.preferences.DebugPanelPreference.Companion.DUMP_HEAP_PANEL
import com.wapo.flagship.features.settings.preferences.DebugPanelPreference.Companion.FIREBASE_DEBUGGER_TITLE
import com.wapo.flagship.features.settings.preferences.DebugPanelPreference.Companion.LONG_AB_TESTS
import com.wapo.flagship.features.settings.preferences.DebugPanelPreference.Companion.ONETRUST_PANEL
import com.wapo.flagship.features.settings.preferences.DebugPanelPreference.Companion.PAYWALL_TITLE
import com.wapo.flagship.features.settings.preferences.DebugPanelPreference.Companion.SHORT_AB_TESTS
import com.wapo.flagship.features.settings.preferences.DebugPanelPreference.Companion.VERIFY_ON_LAUNCH_PANEL
import com.wapo.flagship.util.PrefUtils
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.ConfigOverride
import com.washingtonpost.android.paywall.helper.PaywallPrefHelper

class APIEnvironmentsExpandableListAdapter(
    private val context: Context,
    private val expandableListHeaders: List<String>,
    private val expandableListItems: HashMap<String, List<String>>,
) : BaseExpandableListAdapter() {
    var closeDialog: (() -> Unit)? = null

    private val cookiesJsKeys = setOf("rct")

    private val _selectedConfigOverrides: MutableSet<ConfigOverride> =
        mutableSetOf<ConfigOverride>().apply {
            addAll(ConfigManager.getInstance().state.value?.overrides.orEmpty())
        }
    val selectedConfigOverrides: Set<ConfigOverride> = _selectedConfigOverrides

    private fun getChildren(listPosition: Int): List<String> =
        expandableListItems[expandableListHeaders[listPosition]] ?: emptyList()

    override fun getChild(
        listPosition: Int,
        expandedListPosition: Int,
    ): String? = getChildren(listPosition).getOrNull(expandedListPosition)

    override fun getChildId(
        listPosition: Int,
        expandedListPosition: Int,
    ): Long = expandedListPosition.toLong()

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?,
    ): View? =
        when (val child = getChild(groupPosition, childPosition) as String) {
            DEEP_LINK_PANEL -> initDeepLinkPanel()
            DUMP_HEAP_PANEL -> initDumpHeapPanel(parent)
            AGE_RESTRICTION_V004_PANEL -> initAgeRestrictionV004Panel(parent)
            BLANK_CHECKBOX_PANEL -> initPaywallBypassCheckbox(parent)
            ONETRUST_PANEL -> initOneTrustPanel(parent)
            VERIFY_ON_LAUNCH_PANEL -> initVerifyOnEachLaunchCheckbox(parent)
            else -> {
                when {
                    child.startsWith(CONFIG_ENVIRONMENT_ITEM_PREFIX) -> {
                        val override = ConfigOverride.valueOf(child.removePrefix(CONFIG_ENVIRONMENT_ITEM_PREFIX))
                        initConfigEnvironmentItem(override)
                    }

                    else -> throw Exception("Unknown type $child")
                }
            }
        }

    private fun initOneTrustPanel(parent: ViewGroup?): View {
        val view =
            LayoutInflater.from(context).inflate(
                R.layout.dialog_environments_onetrust_panel,
                parent,
                false,
            )
        val gppString = OneTrustHelper.getGppString(context)?.takeIf { it.isNotBlank() }
        val gppSid = OneTrustHelper.getGppSid(context)?.takeIf { it.isNotBlank() }
        val gppValues = view.findViewById<View>(R.id.onetrust_gpp_values)
        val gppStringContainer = view.findViewById<View>(R.id.onetrust_gpp_string_container)
        val gppSidContainer = view.findViewById<View>(R.id.onetrust_gpp_sid_container)

        gppValues.visibility = if (gppString != null || gppSid != null) View.VISIBLE else View.GONE
        gppStringContainer.visibility = if (gppString != null) View.VISIBLE else View.GONE
        gppSidContainer.visibility = if (gppSid != null) View.VISIBLE else View.GONE
        view.findViewById<TextView>(R.id.onetrust_gpp_string).text = gppString
        view.findViewById<TextView>(R.id.onetrust_gpp_sid).text = gppSid

        val useTestEnvironment = view.findViewById<CheckBox>(R.id.onetrust_stage_toggle)
        useTestEnvironment.isChecked = PrefUtils.getPrefOneTrustStage(context)
        useTestEnvironment.setOnCheckedChangeListener { _, isChecked ->
            OneTrustHelper.clearOneTrustData()
            PrefUtils.setPrefOneTrustStage(context, isChecked)
            val environmentName = if (isChecked) "test" else "production"
            val toast = Toast.makeText(
                context,
                "OneTrust $environmentName selected. Restart the app.",
                Toast.LENGTH_LONG,
            )
            toast.show()
        }
        return view
    }

    private fun initVerifyOnEachLaunchCheckbox(parent: ViewGroup?): View =
        LayoutInflater.from(context).inflate(
            R.layout.dialog_environments_child_item,
            parent,
            false,
        ).apply {
            val checkBox = findViewById<CheckBox>(R.id.checkbox)
            checkBox.isChecked = PrefUtils.getVerifyOnEachLaunch(context)
            checkBox.setOnCheckedChangeListener { _, b ->
                PrefUtils.setVerifyOnEachLaunch(context, b)
                Toast.makeText(
                    context,
                    if (b) {
                        "profile will be verified on every launch. Restart the app."
                    } else {
                        "profile verification back to once every 24h."
                    },
                    Toast.LENGTH_LONG,
                ).show()
            }
            findViewById<TextView>(R.id.itemTxv).text =
                context.getString(R.string.verify_on_each_launch)
        }

    private fun initPaywallBypassCheckbox(parent: ViewGroup?): View =
        LayoutInflater.from(context).inflate(
            R.layout.dialog_environments_child_item,
            parent,
            false,
        ).apply {
            val checkBox = findViewById<CheckBox>(R.id.checkbox)
            checkBox.isChecked = PaywallPrefHelper.getPrefBypassPaywall()
            checkBox.setOnCheckedChangeListener { _, b ->
                PaywallPrefHelper.setBypassPaywall(b)
            }
            findViewById<TextView>(R.id.itemTxv).text = context.getString(R.string.bypass_paywall)
        }

    private fun initDumpHeapPanel(parent: ViewGroup?): View? {
        val convertView =
            LayoutInflater.from(context).inflate(
                R.layout.dialog_environments_leakcanary_dump_heap_panel,
                parent,
                false,
            )
        val checkBox = convertView.findViewById<CheckBox>(R.id.panel_checkbox)
        checkBox.isChecked = PrefUtils.getPrefLeakCanaryDumpHeap(context)
        checkBox.setOnCheckedChangeListener { _, b ->
            Toast.makeText(context, "App restart required", Toast.LENGTH_SHORT).show()
            PrefUtils.setLeakCanaryDumpHeap(context, b)
        }
        return convertView
    }

    private fun initAgeRestrictionV004Panel(parent: ViewGroup?): View? {
        val convertView = LayoutInflater.from(context).inflate(
            R.layout.panel_age_restrictions_v004,
            parent,
            false,
        )
        val fakeEnabled = convertView.findViewById<CheckBox>(R.id.checkbox_fake_v004)
        val errorApi = convertView.findViewById<EditText>(R.id.error_api_v004)
        val accessStatus = convertView.findViewById<Spinner>(R.id.spinner_access_status_v004)
        val significantChangeStatus =
            convertView.findViewById<Spinner>(R.id.spinner_significant_change_status_v004)
        val upperAge = convertView.findViewById<EditText>(R.id.upper_age_v004)
        val lowerAge = convertView.findViewById<EditText>(R.id.lower_age_v004)

        accessStatus.adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            AgeSignalsV004AccessStatuses,
        )
        significantChangeStatus.adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            AgeSignalsV004SignificantChangeStatuses,
        )

        convertView.findViewById<Button>(R.id.btn_save_v004).setOnClickListener {
            val setup = AgeRestrictionsFakeSetUp(
                fakeAgeSignalsManagerEnable = fakeEnabled.isChecked,
                errorAPI = errorApi.text.toString().trim(),
                userUpperAge = upperAge.text.toString().trim(),
                userLowerAge = lowerAge.text.toString().trim(),
                accessStatus = accessStatus.selectedItem as String,
                significantChangeStatus = significantChangeStatus.selectedItem as String,
            )
            Toast.makeText(context, "Close and reopen the app to test this response", Toast.LENGTH_SHORT)
                .show()
            PrefUtils.setAgeRestrictionPref(context, setup)
        }

        return convertView
    }

    private fun getShortABTestGroups(): String =
        PrefUtils.getABParametersMap(context)
            .filterKeys { it !in cookiesJsKeys }
            .entries.joinToString(";") { (k, v) -> "$k|$v" }
            .ifEmpty { "(none)" }

    private fun getLongABTestGroups(): String =
        PrefUtils.getABParametersMap(context)
            .filterKeys { it in cookiesJsKeys }
            .entries.joinToString(";") { (k, v) -> "$k|$v" }
            .ifEmpty { "(none)" }


    private fun initDeepLinkPanel(): View {
        val layoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        var convertView = layoutInflater.inflate(R.layout.dialog_environments_deep_link_panel, null)

        val etDeepLink = convertView.findViewById(R.id.et_deep_link) as? EditText
        val btnDeepLink = convertView.findViewById(R.id.btn_deep_link_open) as? Button

        btnDeepLink?.setOnClickListener {
            val link = etDeepLink?.text.toString().trim()
            if (link.isNotEmpty()) {
                DeepLinksProcessor.processAsync(
                    link,
                    context,
                    scope = convertView.findComponentActivity()?.lifecycleScope,
                )
            }
            closeDialog?.invoke()
        }
        return convertView
    }

    private fun initConfigEnvironmentItem(override: ConfigOverride): View {
        val layoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val convertView = layoutInflater.inflate(R.layout.dialog_environments_child_item, null)

        val textView = convertView.findViewById<TextView>(R.id.itemTxv)
        val checkBox = convertView.findViewById<CheckBox>(R.id.checkbox)

        textView.text = override.name.lowercase()
        checkBox.apply {
            isChecked = _selectedConfigOverrides.contains(override)
            setOnClickListener {
                val isSelected = selectedConfigOverrides.contains(override)
                if (isSelected) {
                    _selectedConfigOverrides.remove(override)
                } else {
                    val selectedOverrideInGroup = selectedConfigOverrides
                        .firstOrNull { it.group == override.group }
                    if (selectedOverrideInGroup != null) {
                        _selectedConfigOverrides.remove(selectedOverrideInGroup)
                    }
                    _selectedConfigOverrides.add(override)
                }
                // Save immediately after checkbox change
                saveOverrides()
                notifyDataSetChanged()
            }
        }

        return convertView
    }

    /**
     * Saves the selected config overrides immediately.
     * Called when a checkbox is clicked to persist changes without needing a Done button.
     */
    private fun saveOverrides() {
        val selectedOverrides = _selectedConfigOverrides.toList()
        ConfigManager.getInstance().setOverrides(selectedOverrides)
        // Persist beta webview preference for non-blocking reads
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(
                "beta_webview_override",
                selectedOverrides.contains(ConfigOverride.WEBVIEW_BETA)
            )
            .apply()
    }

    override fun getChildrenCount(listPosition: Int): Int = getChildren(listPosition).size

    override fun getGroup(listPosition: Int): Any = expandableListHeaders[listPosition]

    override fun getGroupCount(): Int = expandableListHeaders.size

    override fun getGroupId(listPosition: Int): Long = listPosition.toLong()

    override fun getGroupView(
        listPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?,
    ): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.dialog_environments_header_item, parent, false)

        val listTitle = getGroup(listPosition) as String
        val isEmpty = getChildrenCount(listPosition) == 0

        val listTitleTextView = view.findViewById<TextView>(R.id.headerTxv)
        val chevronImageView = view.findViewById<ImageView>(R.id.chevronIv)
        val valueTxv = view.findViewById<TextView>(R.id.valueTxv)

        listTitleTextView.text = listTitle

        if (!isEmpty) {
            chevronImageView.visibility = View.VISIBLE
            valueTxv.visibility = View.GONE
            chevronImageView.setOnClickListener(null) // clear any recycled Paywall listener
            chevronImageView.setImageResource(
                if (isExpanded) com.washingtonpost.android.articles.R.drawable.ic_chevrondown16
                else R.drawable.ic_chevronright16
            )
        } else {
            chevronImageView.visibility = View.GONE
            valueTxv.visibility = View.GONE
            chevronImageView.setOnClickListener(null) // clear any recycled listener
            when (listTitle) {
                SHORT_AB_TESTS -> {
                    valueTxv.visibility = View.VISIBLE
                    valueTxv.text = getShortABTestGroups()
                }
                LONG_AB_TESTS -> {
                    valueTxv.visibility = View.VISIBLE
                    valueTxv.text = getLongABTestGroups()
                }
                PAYWALL_TITLE -> {
                    chevronImageView.visibility = View.VISIBLE
                }
                FIREBASE_DEBUGGER_TITLE -> {
                    chevronImageView.visibility = View.VISIBLE
                }
            }
        }

        return view
    }

    override fun hasStableIds(): Boolean = false

    override fun isChildSelectable(
        listPosition: Int,
        expandedListPosition: Int,
    ): Boolean = true
}
