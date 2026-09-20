/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.section.adapters

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.wapo.flagship.features.section.fragments.HeadlineFragment
import com.wapo.flagship.features.section.models.ArticleMeta

class HeadlineFragmentStateAdapter(
    activity: FragmentActivity,
    private val articleMetaList: List<ArticleMeta>
    ) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = articleMetaList.size

    override fun createFragment(position: Int): Fragment {
        return HeadlineFragment().apply {
            arguments = Bundle().apply {
                putParcelable(HeadlineFragment.ARG_OBJECT, articleMetaList[position])
            }
        }
    }

}