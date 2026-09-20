/* Copyright (c) 2021 The Washington Post. All rights reserved. */

package com.wapo.zendesk.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.wapo.zendesk.databinding.FragmentZendeskSuccessBinding
import com.wapo.zendesk.viewmodel.ZendeskDestinationViewModel

class ZendeskSuccessFragment : Fragment() {

    private var _binding: FragmentZendeskSuccessBinding? = null
    private val binding get() = _binding!!
    private val zendeskDestinationViewModel: ZendeskDestinationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentZendeskSuccessBinding.inflate(inflater, container, false)
        binding.doneButton.setOnClickListener {
            zendeskDestinationViewModel.finish()
        }
        binding.anotherRequestButton.setOnClickListener {
            zendeskDestinationViewModel.startFormReSubmission()
        }
        return binding.root
    }
}