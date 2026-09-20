package com.wapo.zendesk.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.wapo.zendesk.databinding.FragmentZendeskImageBinding

class ZendeskImageFragment : DialogFragment() {
    private var _binding: FragmentZendeskImageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentZendeskImageBinding.inflate(inflater, container, false)
        val uri = arguments?.getString(ARG_IMAGE_URI)
        binding.image.setImageURI(Uri.parse(uri))
        binding.image.setOnClickListener {
            dismiss()
        }
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {

        const val ARG_IMAGE_URI = "ARG_IMAGE_URI"

        fun create(imageUri: String): ZendeskImageFragment {
            return ZendeskImageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_IMAGE_URI, imageUri)
                }
            }
        }
    }
}