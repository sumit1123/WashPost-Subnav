package com.wapo.zendesk.fragments

import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import com.wapo.android.commons.util.Logger
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.wapo.zendesk.R
import com.wapo.zendesk.adapter.ImageListAdapter
import com.wapo.zendesk.databinding.FragmentZendeskFormBinding
import com.wapo.zendesk.model.Status
import com.wapo.zendesk.model.ZendeskImage
import com.wapo.zendesk.notification.ZendeskFeedbackNotification
import com.wapo.zendesk.repository.Result
import com.wapo.zendesk.viewmodel.ZendeskDestinationViewModel
import com.wapo.zendesk.viewmodel.ZendeskViewModel
import dagger.hilt.android.AndroidEntryPoint

const val RESULT_IMAGE_SELECTED = 1

@AndroidEntryPoint
class ZendeskFormFragment : Fragment() {
    private var _binding: FragmentZendeskFormBinding? = null
    private val binding get() = _binding!!

    private val zendeskViewModel: ZendeskViewModel by viewModels()
    private val zendeskDestinationViewModel: ZendeskDestinationViewModel by activityViewModels()
    private val imageListAdapter = ImageListAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentZendeskFormBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.zendeskViewModel = zendeskViewModel
        setupAttachButton()
        setupImageList()
        setupSubmitButton()
        setupAnimation()
        setupDescriptionAccessibility()
        appendBetaScreenShot()
        addRequiredTextAccessibility()
        binding.inputSubjectLayout.isEnabled = !zendeskViewModel.zendeskProvider.isBeta
        return binding.root
    }

    private fun addRequiredTextAccessibility() {
        // adds "Required" when Talkback reads the form's input fields
        val required = getString(R.string.required)

        listOf(
            binding.inputEmail to getString(R.string.hint_email),
            binding.inputSubject to getString(R.string.hint_subject),
            binding.inputDescription to getString(R.string.hint_description),
            binding.inputForm to getString(R.string.hint_area)
        ).forEach { (editText, hint) ->
            ViewCompat.setAccessibilityDelegate(editText, object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    nodeInfo: AccessibilityNodeInfoCompat
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, nodeInfo)
                    val label = "$hint, $required"
                    val inputText = (host as? android.widget.EditText)?.text?.toString().orEmpty()
                    nodeInfo.text = if (inputText.isNotEmpty()) "$label, $inputText" else label
                }
            })
        }
    }

    private fun appendBetaScreenShot() {
        if (requireActivity().intent.hasExtra(ZendeskFeedbackNotification.EXTRA_BETA_FEEDBACK)) {
            val zendeskImage =
                ZendeskFeedbackNotification.composeBetaZendeskImage(requireActivity())
            if (zendeskImage != null) {
                zendeskViewModel.addImage(zendeskImage)
            }
        }
    }

    private fun setupFormList(ticketForms: List<String>) {
        binding.inputForm.setAdapter(
            ArrayAdapter(
                requireContext(),
                R.layout.zendesk_form_item,
                ticketForms
            )
        )

        binding.inputForm.setText(ticketForms.find { it.contains("mobile") || it.contains("beta", ignoreCase = true) }, false)

        // add expanded or collapsed state of dropdown menu to Talkback for accessibility
        binding.areaMenu.findViewById<View>(
            com.google.android.material.R.id.text_input_end_icon
        )?.let {
            ViewCompat.setAccessibilityDelegate(it, object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    nodeInfo: AccessibilityNodeInfoCompat
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, nodeInfo)
                    if (binding.inputForm.isPopupShowing) {
                        nodeInfo.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_COLLAPSE)
                    } else {
                        nodeInfo.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_EXPAND)
                    }
                }
            })
        }
    }

    private fun setupAttachButton() {
        binding.buttonAttach.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
            startActivityForResult(intent, RESULT_IMAGE_SELECTED)
        }
        VectorDrawableCompat.create(resources, R.drawable.ic_attach, context?.theme)?.apply {
            binding.buttonAttach.setCompoundDrawablesWithIntrinsicBounds(this, null, null, null)
        }
    }

    /**
     * Updates the state description of the edit description input field to include the character count and
     * reads out the hint (Edit Description) so users know what the field is correlated to.
     * This is for accessibility purposes with Talkback
     */
    private fun setupDescriptionAccessibility() {
        binding.inputDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val charCount = s?.length ?: 0
                val hint = binding.inputDescription.hint ?: ""
                ViewCompat.setStateDescription(
                    binding.inputDescription,
                    "$hint, ${resources.getString(R.string.characters_used_accessibility, charCount)}"
                )
            }
        })
    }

    private fun setupImageList() {
        binding.imageList.apply {
            adapter = imageListAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
        imageListAdapter.onCloseClicked = { zendeskImage, pos ->
            zendeskViewModel.removeImage(zendeskImage)
        }
        imageListAdapter.onImageClicked = { zendeskImage, pos ->
            openFullscreen(zendeskImage)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        zendeskViewModel.imageLiveData.observe(viewLifecycleOwner) { list ->
            onListChanged(list)
        }
        zendeskViewModel.imageCounterLiveData.observe(viewLifecycleOwner) { counter ->
            onCounterChanged(counter)
        }
        zendeskViewModel.ticketFormsLiveData.observe(viewLifecycleOwner) { ticketForms ->
            if (ticketForms != null) {
                setupFormList(ticketForms.map { it.second })
            }
        }
        zendeskViewModel.loadingLiveData.observe(viewLifecycleOwner) { isLoading ->
            binding.scrollView.apply {
                if (!isLoading && visibility == View.GONE) {
                    visibility = View.VISIBLE
                }
            }
            binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        zendeskViewModel.resultLiveData.observe(viewLifecycleOwner, {
            when (it) {
                Result.Success -> zendeskDestinationViewModel.formSubmissionSuccessful()
                else -> {
                    if (it != null) {
                        context?.let { context ->
                            AlertDialog.Builder(context)
                                .setMessage("Request sending failed. Please try again").show()
                        }
                    }
                }
            }
        })
        zendeskViewModel.init(requireContext())
        zendeskViewModel.getTicketForms()
    }

    private fun onListChanged(list: MutableSet<ZendeskImage>?) {
        if (list != null && list.size > 0) {
            imageListAdapter.submitList(list.toMutableList())
            binding.imageList.visibility = View.VISIBLE
            val imagesUploadingInProgress = list.any { it.status != Status.IDLE }
            binding.buttonAttach.isEnabled = !imagesUploadingInProgress
            binding.buttonSubmit.isEnabled = !imagesUploadingInProgress
        } else {
            imageListAdapter.submitList(null)
            binding.imageList.visibility = View.GONE
        }
    }

    private fun onCounterChanged(counter: Int) {
        if (counter > 0) {
            val counterText =
                if (counter > 1) getString(R.string.images_attached, counter)
                else getString(R.string.image_attached, counter)
            binding.imageCounter.text = counterText
            binding.imageCounter.visibility = View.VISIBLE
        } else {
            binding.imageCounter.visibility = View.GONE
        }
    }

    private fun setupSubmitButton() {
        binding.buttonSubmit.setOnClickListener {
            zendeskViewModel.createRequest(
                binding.inputForm.text.toString(),
                binding.inputEmail.text.toString(),
                binding.inputSubject.text.toString(),
                binding.inputDescription.text.toString()
            )
        }
    }

    private fun setupAnimation() {
        binding.root.layoutTransition.setDuration(800L)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_IMAGE_SELECTED) {
            if (activity?.isFinishing == false) {
                var uri: String? = null
                var fileName: String? = null
                var mimeType: String? = null
                data?.data?.let { returnUri ->
                    uri = returnUri.toString()
                    mimeType = requireActivity().contentResolver.getType(returnUri)
                    requireActivity().contentResolver.query(returnUri, null, null, null, null)
                }?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    fileName = cursor.getString(nameIndex)
                }
                val result = listOfNotNull(uri, fileName, mimeType)
                if (result.size == 3) {
                    zendeskViewModel.addImage(ZendeskImage(result[0], result[1], result[2]))
                } else {
                    Logger.e(TAG, "Error selecting image")
                }
            }
        }
    }

    private fun openFullscreen(zendeskImage: ZendeskImage) {
        val fragment = ZendeskImageFragment.create(zendeskImage.uri)
        fragment.show(childFragmentManager, null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val TAG: String = ZendeskFormFragment::class.java.simpleName
    }
}