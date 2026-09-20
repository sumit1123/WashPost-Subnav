package com.wapo.flagship.features.ask.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import com.wapo.flagship.features.ask.ui.VoiceSelectionView
import com.wapo.flagship.features.ask.viewmodels.TalkToThePostVoiceSelectionViewModel
import com.wapo.flagship.util.PrefUtils
import com.wapo.fragment.BaseBottomSheetDialogFragment
import com.wpds.theme.AndroidClassicTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TalkToThePostVoiceSelectionBottomSheetFragment : BaseBottomSheetDialogFragment() {

    private val talkToThePostVoiceSelectionViewModel: TalkToThePostVoiceSelectionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        makeBackgroundTransparent()
        return ComposeView(requireContext()).apply {
            setContent {
                AndroidClassicTheme {
                    Surface(
                        color = Color.Transparent,
                    ) {
                        VoiceSelectionView(
                            PrefUtils.getTalkToThePostVoiceSelection(context),
                            { onItemSelected(it) },
                            { dismiss() }
                        )
                    }
                }
            }
        }
    }

    private fun onItemSelected(id: String) {
        PrefUtils.setTalkToThePostVoiceSelection(requireContext(), id)
        talkToThePostVoiceSelectionViewModel.playVoiceSelection(id)
    }

    override fun onDestroy() {
        talkToThePostVoiceSelectionViewModel.destroy()
        super.onDestroy()
    }

    companion object {
        val TAG = TalkToThePostVoiceSelectionBottomSheetFragment::class.simpleName
    }
}
