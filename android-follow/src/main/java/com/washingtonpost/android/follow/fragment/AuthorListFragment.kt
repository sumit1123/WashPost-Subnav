package com.washingtonpost.android.follow.fragment

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import com.washingtonpost.android.follow.activity.FollowActivity
import com.washingtonpost.android.follow.database.model.AuthorEntity
import com.washingtonpost.android.follow.databinding.AuthorListFragmentBinding
import com.washingtonpost.android.follow.misc.FollowTrackingInfo
import com.washingtonpost.android.follow.misc.TrackingEvent
import com.washingtonpost.android.follow.ui.*
import com.washingtonpost.android.follow.ui.adapter.AuthorsAdapter
import com.washingtonpost.android.follow.viewmodel.ArticleListViewModel
import com.washingtonpost.android.follow.viewmodel.FollowViewModel
import com.washingtonpost.android.follow.viewmodel.ViewModelHelper

class AuthorListFragment : Fragment() {
    private var isAutoSelect = true
    private val followViewModel by ViewModelHelper.getViewModel(this, FollowViewModel::class)
    private val articleListViewModel by ViewModelHelper.getViewModel(this, ArticleListViewModel::class)
    private lateinit var selectionTracker: SelectionTracker<AuthorEntity>
    private lateinit var authorLayoutManager: AuthorLayoutManager
    private lateinit var authorsAdapter: AuthorsAdapter
    private val handler = Handler()
    private var followId: String = ""

    private var _binding: AuthorListFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = AuthorListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FollowTrackingInfo.followTracking.appSection = "following"
        binding.list.apply {
            authorLayoutManager = AuthorLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            layoutManager = authorLayoutManager
            authorsAdapter = AuthorsAdapter(followViewModel.followManager.followProvider)
            adapter = authorsAdapter
            selectionTracker = SelectionTracker.Builder(
                    "authorSelectionId",
                    this,
                    AuthorKeyProvider(authorsAdapter),
                    AuthorDetailsLookup(this),
                    StorageStrategy.createParcelableStorage(AuthorEntity::class.java)
            )
                    .withSelectionPredicate(SelectionPredicates.createSelectSingleAnything())
                    .withOnItemActivatedListener { itemDetails, _ ->
                        itemDetails.selectionKey?.let {
                            selectionTracker.select(it)
                        }
                        return@withOnItemActivatedListener true
                    }
                    .build()
            savedInstanceState?.let {
                selectionTracker.onRestoreInstanceState(it)
            }
            authorsAdapter.selectionTracker = selectionTracker
            authorsAdapter.selectionListener = object : AuthorsAdapter.AuthorSelectionListener {
                override fun onAuthorSelected(authorEntity: AuthorEntity) {
                    if (!isAutoSelect) {
                        FollowTrackingInfo.followTracking.contentAuthor = authorEntity.name
                        followViewModel.followManager.followProvider.onTrackingEvent(TrackingEvent.ON_AUTHOR_SELECTED)
                    }
                    isAutoSelect = false
                    followViewModel.setPosition(authorEntity.authorId)
                    articleListViewModel.showArticles(authorEntity.authorId)
                    followId = authorEntity.authorId
                }
            }
            followViewModel.followedAuthors?.observe(viewLifecycleOwner, Observer {
                authorsAdapter.submitList(it) {
                    if (!selectionTracker.hasSelection()) {
                        if (!followId.isBlank()) {
                            followViewModel.setPosition(followId)
                        } else {
                            authorsAdapter.currentList?.apply {
                                if (isNotEmpty()) {
                                    selectionTracker.select(first())
                                }
                            }
                        }
                    } else {
                        authorsAdapter.selectedItem?.let { authorEntity ->
                            followViewModel.setPosition(authorEntity.authorId)
                        }
                    }
                    authorLayoutManager.isScrollEnabled = it.isNotEmpty()
                    authorsAdapter.setEmptyRows()
                    if (it.isEmpty() && activity?.isFinishing == false) {
                        (activity as? FollowActivity)?.showToolbars()
                    }
                }
            })
            followViewModel.authorPosition.observe(viewLifecycleOwner, Observer { position ->
                handlePosition(position ?: 0)
            })
            addItemDecoration(AuthorListMarginItemDecoration(context))
        }
        observeDeepLinkedAuthorId()
    }

    private fun handlePosition(position: Int) {
        if (position >= 0 && position < authorsAdapter.itemCount) {
            // the delay is needed to prevent scroll issues when the order of the list
            // changes significantly. an easy way to test this is to set the author lmt
            // to random values when selected. additionally, suppress layout is used to
            // prevent the user from scrolling during this short period.
            handler.apply {
                removeCallbacksAndMessages(null)
                post {
                    if (isFinishing()) {
                        binding.list.suppressLayout(true)
                    }
                }
                postDelayed({
                    if (isFinishing()) {
                        authorLayoutManager.startSmoothScroll(CenterSmoothScroller(binding.list.context, position) {
                            if (isFinishing()) {
                                authorsAdapter.getAuthor(position)?.let { authorEntity ->
                                    selectionTracker.select(authorEntity)
                                }
                                binding.list.suppressLayout(false)
                            }
                        })
                    }
                }, AUTHOR_SCROLL_DELAY)
            }
        }
    }

    private fun isFinishing(): Boolean {
        return activity?.isFinishing == false
    }

    fun setFollowId(followId: String?) {
        if (followId != null) {
            this.followId = followId
            selectionTracker.clearSelection()
            followViewModel.setPosition(followId)
        }
    }

    fun observeDeepLinkedAuthorId() {
        followViewModel.deepLinkedAuthorId.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                setFollowId(it)
                followViewModel.setDeepLinkedAuthorId("")
            }
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
        private const val AUTHOR_SCROLL_DELAY = 200L

        @JvmStatic
        fun newInstance() = AuthorListFragment()
    }
}