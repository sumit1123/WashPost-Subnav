package com.washingtonpost.android.follow.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.washingtonpost.android.follow.R
import com.washingtonpost.android.follow.databinding.MyPostFollowFragmentBinding
import com.washingtonpost.android.follow.misc.TrackingEvent
import com.washingtonpost.android.follow.viewmodel.FollowViewModel
import com.washingtonpost.android.follow.viewmodel.ViewModelHelper
import com.washingtonpost.android.paywall.PaywallService

class MyPostFollowFragment : Fragment() {
    private val followViewModel by ViewModelHelper.getViewModel(this, FollowViewModel::class)
    private val authorListFragment = AuthorListFragment.newInstance()
    private var _binding: MyPostFollowFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = MyPostFollowFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        followViewModel.followManager.followProvider.onTrackingEvent(TrackingEvent.ON_VISIT_FOLLOWING_FEED)
        childFragmentManager
                .beginTransaction()
                .add(R.id.author_list, authorListFragment)
                .add(R.id.article_list, ArticleListFragment.newInstance(false))
                .commit()

        followViewModel.numFollowing.observe(viewLifecycleOwner, Observer {
            if (it > 0 && PaywallService.getInstance().loggedInUser != null) {
                binding.emptyState.myPostFollowEmptyState.visibility = View.GONE
                binding.authorList.visibility = View.VISIBLE
                binding.articleList.visibility = View.VISIBLE
                binding.articleList.isEnabled = true
            } else {
                binding.emptyState.myPostFollowEmptyState.visibility = View.VISIBLE
                updateEmptyState()
                binding.authorList.visibility = View.GONE
                binding.articleList.visibility = View.GONE
                binding.articleList.isEnabled = false
            }
        })
    }

    override fun onResume() {
        super.onResume()
        updateEmptyState()
    }

    fun setFollowId(followId: String?) {
        authorListFragment.setFollowId(followId)
    }

    private fun updateEmptyState() {
        val ctx = requireContext()
        if (followViewModel.followManager.followProvider.isLoggedInUserAndSubscriber()) {
            binding.emptyState.tvMsgTitle.text =
                ctx.getString(R.string.my_post_es_following_signed_in_title)
            binding.emptyState.tvMsgDesc.text =
                ctx.getString(R.string.my_post_es_following_signed_in_desc)
            binding.emptyState.buttonOpen.visibility = View.GONE
        } else {
            binding.emptyState.tvMsgTitle.text =
                ctx.getString(R.string.my_post_es_following_sign_in_title)
            binding.emptyState.tvMsgDesc.text =
                ctx.getString(R.string.my_post_es_following_sign_in_desc)
            binding.emptyState.buttonOpen.apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    followViewModel.followManager.followProvider.handleSignInOrCreateAccount(
                        requireActivity()
                    )
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = MyPostFollowFragment()
    }
}