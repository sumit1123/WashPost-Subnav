package com.washingtonpost.customnav.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.washingtonpost.customnav.R
import com.washingtonpost.customnav.databinding.FragmentCustomNavBinding
import com.washingtonpost.customnav.viewmodel.CustomNavViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomNavFragment : Fragment() {

    private val viewModel: CustomNavViewModel by viewModels()
    private lateinit var binding: FragmentCustomNavBinding
    private val activeSectionsAdapter = CustomNavAdapter()
    private val recommendedSectionsAdapter = CustomNavAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        viewModel.trackCustomNavPageView()
        binding = FragmentCustomNavBinding.inflate(inflater)
        initCellButtonCallbacks()
        initDragToReorder()
        initLayoutManagers()
        binding.activeRv.adapter = activeSectionsAdapter
        binding.recommendedRv.adapter = recommendedSectionsAdapter
        return binding.root
    }

    private fun initCellButtonCallbacks() {
        activeSectionsAdapter.onButtonClicked = { section ->
            viewModel.removeSection(section)
        }
        recommendedSectionsAdapter.onButtonClicked = { section ->
            viewModel.addSection(section)
        }
    }

    private fun initDragToReorder() {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(UP + DOWN, 0) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return viewModel.reorderSections(
                    viewHolder.bindingAdapterPosition,
                    target.bindingAdapterPosition
                )
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun isLongPressDragEnabled(): Boolean {
                return true
            }
        }).attachToRecyclerView(binding.activeRv)
    }

    private fun initLayoutManagers() {
        if (binding.activeRv.layoutManager == null) {
            binding.activeRv.layoutManager = GridLayoutManager(context, 1)
        }
        if (binding.recommendedRv.layoutManager == null) {
            val flexboxLayoutManager = FlexboxLayoutManager(context)
            flexboxLayoutManager.flexWrap = FlexWrap.WRAP
            binding.recommendedRv.layoutManager = flexboxLayoutManager
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.activeSections.observe(viewLifecycleOwner) {
            activeSectionsAdapter.setItems(it)
        }
        viewModel.displayedRecommendedSections.observe(viewLifecycleOwner) {
            recommendedSectionsAdapter.setItems(it)
        }
        binding.resetButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage(resources.getString(R.string.reset_confirmation))
                .setCancelable(true)
                .setPositiveButton(resources.getString(R.string.reset_cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setNegativeButton(resources.getString(R.string.reset_confirm)) { _,_ ->
                    viewModel.resetTopics()
                }
                .show()
        }
    }

    /**
     * Call tracking for section customization only when we are leaving the fragment.
     */
    override fun onPause() {
        super.onPause()
        viewModel.trackEnrollDisenroll()
    }
}