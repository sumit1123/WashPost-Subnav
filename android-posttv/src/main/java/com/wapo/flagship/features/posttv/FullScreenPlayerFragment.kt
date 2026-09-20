/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.posttv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import com.wapo.flagship.features.posttv.databinding.FullSreenPlayerFragmentBinding
import com.wapo.flagship.features.posttv.listeners.PiPActivity

/**
 * Class to handle Player's FullScreen UI (Immersive).
 */
class FullScreenPlayerFragment : DialogFragment() {

    private lateinit var viewBinding: FullSreenPlayerFragmentBinding
    private var playerManager: PostTvPlayer2Manager? = null

    init {
        arguments = if (arguments == null) Bundle() else arguments
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog?.setCanceledOnTouchOutside(false)
        if (savedInstanceState != null) dismiss()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FullSreenPlayerFragmentBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideSystemBars()
        getPlayerName()?.let { playerName ->
            playerManager = PostTvPlayer2Coordinator.getPlayer(playerName).also {
                it.addPlayerContainerViewToItemView(viewBinding.mediaContainer)
                it.setControllerVisibilityListener { visibility ->
                    when (visibility) {
                        View.VISIBLE -> viewBinding.backButton.visibility = View.VISIBLE
                        else -> {
                            if (it.player2ViewModel?.errorState == false) {
                                viewBinding.backButton.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }
        setupBackButton()
    }

    override fun onDetach() {
        playerManager?.exitFullScreen()
        playerManager = null
        super.onDetach()
    }

    fun setPlayerName(name: String?) {
        arguments?.putString(PLAYER_NAME, name)
    }

    fun getPlayerName(): String? {
        return arguments?.getString(PLAYER_NAME)
    }

    private fun setupBackButton() {
        viewBinding.backButton.setOnClickListener {
            if (activity is PiPActivity) {
                (activity as PiPActivity).enterPiPMode()
            } else {
                dismiss()
            }
        }
        viewBinding.backButton.bringToFront()
    }

    private fun hideSystemBars() {
        val window = dialog?.window ?: return
        val windowInsetsController =
            ViewCompat.getWindowInsetsController(window.decorView) ?: return
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    companion object {
        const val PLAYER_NAME = "PLAYER_NAME"
    }
}