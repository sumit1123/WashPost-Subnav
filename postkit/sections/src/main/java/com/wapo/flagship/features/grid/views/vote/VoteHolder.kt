package com.wapo.flagship.features.grid.views.vote

import android.view.View
import com.wapo.flagship.features.grid.GridAdapter
import com.wapo.flagship.features.grid.GridViewHolder
import com.wapo.flagship.features.grid.model.Vote
import com.washingtonpost.android.sections.R
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class VoteHolder(itemView: View) : GridViewHolder(itemView) {

    private val voteView = itemView.findViewById(R.id.voteView) as VoteView

    override fun bind(position: Int, gridAdapter: GridAdapter) {
        if (voteView.getVoteGuide() == null) {
            loadVoteGuide(gridAdapter)
        }
        voteView.onRetryClicked = {
            loadVoteGuide(gridAdapter)
        }
        voteView.onLinkClicked = { url ->
            gridAdapter.environment.onVoteGuideClicked(url)
        }
    }

    private fun loadVoteGuide(gridAdapter: GridAdapter) {
        voteView.isLoading = true
        gridAdapter.environment.getVoteGuideService().getVoteGuide()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {
                            voteView.setGuide(it)
                        },
                        {
                            voteView.setError(it)
                        }
                )
    }
}