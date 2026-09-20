package com.washingtonpost.android.follow.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.washingtonpost.android.follow.databinding.ArticleListFragmentBinding
import com.washingtonpost.android.follow.repository.NetworkState
import com.washingtonpost.android.follow.ui.ArticleListDividerItemDecoration
import com.washingtonpost.android.follow.ui.ArticleListMarginItemDecoration
import com.washingtonpost.android.follow.ui.CardifiedListTopMarginDecoration
import com.washingtonpost.android.follow.ui.adapter.ArticlesAdapter
import com.washingtonpost.android.follow.viewmodel.ArticleListViewModel
import com.washingtonpost.android.follow.viewmodel.FollowViewModel
import com.washingtonpost.android.follow.viewmodel.ViewModelHelper

class ArticleListFragment : Fragment(), ArticlesAdapter.ArticleClickListener {
    private val followViewModel by ViewModelHelper.getViewModel(this, FollowViewModel::class)
    private val articleListViewModel by ViewModelHelper.getViewModel(this, ArticleListViewModel::class)
    private var adapter: ArticlesAdapter? = null
    private var shouldHandleEmptyState = false
    private var _binding: ArticleListFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shouldHandleEmptyState = arguments?.getBoolean(PARAM_EMPTY_STATE) ?: false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = ArticleListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapter()
        initSwipeToRefresh()
    }

    private fun initAdapter() {
        adapter = ArticlesAdapter(articleListViewModel.followManager.followProvider,
            this,
            {
                articleListViewModel.retry()
            },
            {
                followViewModel.handleUtilityMenuClickEvent(it)
            }
        )
        binding.list.adapter = adapter
        binding.list.apply {
            val addSideMargins = requireArguments().getInt(PARAM_SOURCE) == Source.AUTHOR_PAGE.ordinal
            addItemDecoration(CardifiedListTopMarginDecoration(context))
            addItemDecoration(ArticleListDividerItemDecoration(context, RecyclerView.HORIZONTAL))
            addItemDecoration(ArticleListMarginItemDecoration(context, RecyclerView.HORIZONTAL))
            addItemDecoration(ArticleListMarginItemDecoration(context, RecyclerView.VERTICAL, allowEdgeItems = addSideMargins))
        }
        articleListViewModel.articles.observe(viewLifecycleOwner, Observer {
            adapter?.submitList(it) {
                // Workaround for an issue where RecyclerView incorrectly uses the loading / spinner
                // item added to the end of the list as an anchor during initial load.
                val layoutManager = (binding.list.layoutManager as LinearLayoutManager)
                val position = layoutManager.findFirstCompletelyVisibleItemPosition()
                if (position != RecyclerView.NO_POSITION) {
                    binding.list.scrollToPosition(position)
                }
            }
        })
        articleListViewModel.networkState.observe(viewLifecycleOwner, Observer {
            adapter?.setNetworkState(it)
        })
        if (shouldHandleEmptyState) {
            followViewModel.numFollowing.observe(viewLifecycleOwner, Observer {
                if (it > 0) {
                    binding.swipeRefresh.visibility = View.VISIBLE
                    binding.list.isEnabled = true
                } else {
                    binding.swipeRefresh.visibility = View.GONE
                    binding.list.isEnabled = false
                }
            })
        }
    }

    private fun initSwipeToRefresh() {
        articleListViewModel.refreshState.observe(viewLifecycleOwner, Observer {
            binding.swipeRefresh.isRefreshing = it == NetworkState.LOADING
        })
        binding.swipeRefresh.setOnRefreshListener {
            articleListViewModel.refresh()
        }
    }

    override fun onArticleClicked(url: String) {
        if (activity != null && activity?.isFinishing == false) {
            val urls = adapter?.currentList?.mapNotNull { it.url } ?: emptyList()
            followViewModel.followManager.followProvider.openArticles(context, urls.toTypedArray(), url)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val PARAM_EMPTY_STATE = "emptyState"
        private const val PARAM_SOURCE = "source"

        enum class Source {
            AUTHOR_PAGE,
            MY_POST
        }

        @JvmStatic
        fun newInstance(shouldHandleEmptyState: Boolean, source: Source = Source.MY_POST) =
                ArticleListFragment().apply {
                    arguments = Bundle().apply {
                        putBoolean(PARAM_EMPTY_STATE, shouldHandleEmptyState)
                        putInt(PARAM_SOURCE, source.ordinal)
                    }
                }
    }
}