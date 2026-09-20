package com.washingtonpost.android.follow.fragment

import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.wapo.text.WpTextFormatter
import com.washingtonpost.android.follow.R
import com.washingtonpost.android.follow.databinding.AuthorBottomSheetDialogFragmentBinding
import com.washingtonpost.android.follow.helper.AuthorProvider
import com.washingtonpost.android.follow.misc.FollowTrackingInfo
import com.washingtonpost.android.follow.misc.TrackingEvent
import com.washingtonpost.android.follow.viewmodel.AuthorBottomSheetDialogViewModel
import com.washingtonpost.android.follow.viewmodel.FollowViewModel
import com.washingtonpost.android.follow.viewmodel.ViewModelHelper
import com.washingtonpost.android.paywall.PaywallService

class AuthorBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private val authorViewModel: AuthorBottomSheetDialogViewModel by activityViewModels()
    private val followViewModel by ViewModelHelper.getViewModel(this, FollowViewModel::class)
    private val handler: Handler = Handler()
    private var shouldShowSnackbar = false
    private var _binding: AuthorBottomSheetDialogFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = AuthorBottomSheetDialogFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authorViewModel.author.observe(viewLifecycleOwner, Observer { author ->
            if (author != null) {
                binding.apply {
                    if (authorViewModel.isLowDataModeEnable.value == true) {
                        authorImage.isVisible = false
                    } else {
                        authorImage.isVisible = true
                        authorImage.apply {
                            followViewModel.followManager.followProvider.getAuthorImageRequestUrl(author.image)?.apply {
                                setImageUrl(this, followViewModel.followManager.followProvider.animatedImageLoader)
                            }
                            setPlaceholder(R.drawable.author_placeholder)
                            setErrorDrawable(R.drawable.author_placeholder)
                        }
                    }
                    WpTextFormatter.applyLineSpacing(authorName, R.style.author_bottom_sheet_name)
                    authorName.text = author.name
                    authorName.setOnClickListener {
                        FollowTrackingInfo.followTracking.authorId = author.id
                        followViewModel.followManager.followProvider.onTrackingEvent(TrackingEvent.ON_AUTHOR_PAGE_OPEN_FROM_CARD)
                        followViewModel.followManager.followProvider.onAuthorNameClicked(author)
                        followViewModel.followManager.followProvider.startAuthorPageActivity(requireActivity(), author)
                    }
                    val description = author.expertise ?: author.bio
                    if (description?.isNotBlank() == true) {
                        WpTextFormatter.applyLineSpacing(authorBio, R.style.author_bottom_sheet_bio)
                        authorBio.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY)
                        } else {
                            Html.fromHtml(description)
                        }
                        authorBio.movementMethod = LinkMovementMethod.getInstance()
                    }

                    if (PaywallService.getInstance().loggedInUser != null) {
                        followViewModel.isFollowing(author).observe(this@AuthorBottomSheetDialogFragment, Observer { followEntity ->
                            val isFollowing = followEntity != null
                            authorFollowButton.isEnabled = true
                            if (isFollowing) {
                                authorFollowButton.text = resources.getString(R.string.author_button_following)
                                VectorDrawableCompat.create(resources, R.drawable.ic_active_follow, context?.theme)?.apply {
                                    authorFollowButton.setCompoundDrawablesWithIntrinsicBounds(null, null, this, null)
                                }
                            } else {
                                authorFollowButton.text = resources.getString(R.string.author_button_follow)
                                VectorDrawableCompat.create(resources, R.drawable.ic_inactive_follow, context?.theme)?.apply {
                                    authorFollowButton.setCompoundDrawablesWithIntrinsicBounds(null, null, this, null)
                                }
                            }
                            authorFollowButton.isSelected = isFollowing
                            authorFollowButton.setOnClickListener {
                                authorFollowButton.isEnabled = false
                                followViewModel.setFollowing(!isFollowing, author) { reachedMaxFollow ->
                                    if (reachedMaxFollow) {
                                        followViewModel.followManager.followProvider.onMaxFollowReached(context, author)
                                        authorFollowButton.isEnabled = true
                                    } else {
                                        shouldShowSnackbar = !isFollowing
                                        handler.apply {
                                            removeCallbacksAndMessages(null)
                                            postDelayed({
                                                FollowTrackingInfo.followTracking.miscellany = TRACKING_AUTHOR_CARD
                                                val trackingEvent = if (isFollowing) TrackingEvent.ON_UNFOLLOWED else TrackingEvent.ON_FOLLOWED
                                                followViewModel.followManager.followProvider.onTrackingEvent(trackingEvent)
                                                if (!isFollowing) {
                                                    dismissAllowingStateLoss()
                                                }
                                            }, DISMISS_DELAY_MS)
                                        }
                                    }
                                }
                            }
                        })
                    } else {
                        authorFollowButton.visibility = View.GONE
                    }
                }
            } else {
                dismissAllowingStateLoss()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(binding.authorBottomSheet.parent as ViewGroup).apply {
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (shouldShowSnackbar) {
            followViewModel.followManager.followProvider.onAuthorFollowed((context as AuthorProvider).getRootView(), authorViewModel.author.value?.id)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val DISMISS_DELAY_MS = 500L
        private const val TRACKING_AUTHOR_CARD = "author_card"
        fun newInstance() = AuthorBottomSheetDialogFragment()
    }
}
