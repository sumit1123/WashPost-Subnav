package com.wapo.flagship.features.settings

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.wapo.flagship.Utils
import com.wapo.flagship.data.ArchiveManager
import com.wapo.flagship.util.ConnectivityMonitor.Companion.getInstance
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class SettingsStorageFragment :
    BasePreferenceFragmentCompat(),
    Preference.OnPreferenceClickListener,
    Preference.OnPreferenceChangeListener {
    private val storageViewModel: SettingsStorageViewModel by viewModels()

    var prefBackgroundSync: ListPreference? = null

    var prefDeletePrint: Preference? = null

    var prefLowDataMode: Preference? = null

    private lateinit var applicationContext: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        applicationContext = context
    }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        setPreferencesFromResource(R.xml.pref_settings_storage, rootKey)

        // Set listeners
        prefBackgroundSync =
            findPreference<ListPreference>(AppPreferences.PREF_BACKGROUND_SYNC)?.apply {
                onPreferenceChangeListener = this@SettingsStorageFragment
            }
        findPreference<Preference>(AppPreferences.PREF_CUSTOMIZE_PRINT_SECTIONS)?.onPreferenceClickListener = this
        prefDeletePrint =
            findPreference<Preference>(AppPreferences.PREF_DELETE_PRINT)?.apply {
                onPreferenceClickListener = this@SettingsStorageFragment
            }
        prefLowDataMode =
            findPreference<Preference>(AppPreferences.PREF_LOW_DATA_MODE)?.apply {
                onPreferenceClickListener = this@SettingsStorageFragment
            }

        if (!ConfigManager.getInstance().config.lowDataModeConfig.enable) {
            preferenceScreen.removePreferenceRecursively(
                resources.getString(R.string.pref_storage_low_data_mode),
            )
        }

        updateBackgroundSyncPreference()
        updateDeletePrintPreference()
        updateAutoplayVideoAllowed()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        observeDeleteStateEvents()
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            AppPreferences.PREF_CUSTOMIZE_PRINT_SECTIONS -> {
                Navigation.findNavController(requireView()).navigate(R.id.printSettingsFragment)
                return true
            }
            AppPreferences.PREF_DELETE_PRINT -> {
                DeletePrompt(storageViewModel).show(parentFragmentManager, "delete_prompt")
                return true
            }
            AppPreferences.PREF_LOW_DATA_MODE -> {
                AppPreferences.setIsLowDataModeEnabledFromSettings(
                    AppPreferences.isLowDataModeEnabled(),
                )
                Measurement.trackSettingsPageViewToggleEvent(
                    AppPreferences.isLowDataModeEnabled(),
                    "lite_mode",
                )
                return true
            }
        }
        return false
    }

    override fun onPreferenceChange(
        preference: Preference,
        newValue: Any?,
    ): Boolean {
        when (preference.key) {
            AppPreferences.PREF_BACKGROUND_SYNC -> {
                updateBackgroundSyncPreference(newValue as? String)
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        updateAutoplayVideoAllowed()
    }

    private fun updateBackgroundSyncPreference(backgroundSyncValue: String? = AppPreferences.getBackgroundSync()) {
        prefBackgroundSync?.summary = getBackgroundSyncSummary(backgroundSyncValue)
    }

    private fun updateDeletePrintPreference() {
        val pdfSize = ArchiveManager.getPrintEditionSizeInDisk(requireContext())
        prefDeletePrint?.title = "Delete downloaded print editions (${String.format(
            Locale.US,
            "%.2f",
            pdfSize,
        )}MB)"
    }

    private fun observeDeleteStateEvents() {
        storageViewModel.deleteState.observe(
            viewLifecycleOwner,
            Observer {
                updateDeletePrintPreference()
            },
        )
    }

    /**
     * If Data Saver is enabled and the device is on a metered network,
     * disable the autoplay video toggle and add the unavailable blurb.
     * Note: Data Saver is available on API 24 and greater,
     * so APIs below 24 are treated as if Data Saver is always enabled.
     */
    private fun updateAutoplayVideoAllowed() {
        val prefAutoplayVideos =
            findPreference<SwitchPreferenceCompat>(
                AppPreferences.PREF_AUTOPLAY_VIDEOS,
            )
        val autoplayAllowed = !getInstance(applicationContext).hasDeviceLevelDataRestriction()
        prefAutoplayVideos?.summary =
            if (autoplayAllowed) {
                ""
            } else {
                applicationContext.resources.getString(
                    R.string.video_autoplay_unavailable_when_data_saver_enabled,
                )
            }
        prefAutoplayVideos?.isEnabled = autoplayAllowed
    }

    class DeletePrompt(
        private val storageViewModel: SettingsStorageViewModel,
    ) : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog
                .Builder(requireContext())
                .apply {
                    setMessage(R.string.print_delete_warning)
                        .setPositiveButton(R.string.continue_delete) { _, _ ->
                            try {
                                storageViewModel.deletePrintFiles()
                            } catch (e: Exception) {
                                Logger.w(TAG, Utils.exceptionToString(e))
                            }
                        }.setNegativeButton(com.washingtonpost.android.notifications.R.string.cancelLabel) { _, _ -> }
                }.create()
    }

    private fun getBackgroundSyncSummary(value: String?): String? {
        println("StorageDebug: value=$value")
        return when (value) {
            "900" -> "Every 15 minutes"
            "3600" -> "Every 1 hour"
            "14400" -> "Every 4 hours"
            "43200" -> "Every 12 hours"
            "86400" -> "Every 24 hours"
            else -> "Never"
        }
    }

    companion object {
        const val TAG = "SettingsStorageFragment"
    }
}
