package com.komik.komik.ui.riwayat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.komik.komik.service.utils.isTablet
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.komik.komik.MainActivity
import com.komik.komik.R
import com.komik.komik.databinding.FragmentHistoryBinding
import com.komik.komik.service.adapter.AdapterRiwayat
import com.komik.komik.service.component.AlertDialogCostum
import com.komik.komik.service.data.model.RiwayatDetail

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HistoryViewmodel
    private lateinit var adapter: AdapterRiwayat

    private var currentList: List<RiwayatDetail> = emptyList()
    private var currentQuery: String = ""
    private var selectedSource: String = "Semua"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[HistoryViewmodel::class.java]

        setupRecyclerView()
        setupSearchAndFilter()
        setupHeaderActions()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = AdapterRiwayat(
            rawList = emptyList(),
            onDeleteClick = { item ->
                showDeleteSingleConfirmation(item)
            }
        )

        // Tablet sw600dp: 2-column grid with headers spanning 2 columns
        if (requireContext().isTablet()) {
            val gridLayoutManager = GridLayoutManager(requireContext(), 2).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return when (adapter.getItemViewType(position)) {
                            AdapterRiwayat.VIEW_TYPE_HEADER -> 2
                            else -> 1
                        }
                    }
                }
            }
            binding.listRiwayat.layoutManager = gridLayoutManager
        } else {
            binding.listRiwayat.layoutManager = LinearLayoutManager(requireContext())
        }
        binding.listRiwayat.adapter = adapter

        // Swipe to delete
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                // Disable swipe on time headers
                if (viewHolder is AdapterRiwayat.HeaderViewHolder) return 0
                return super.getSwipeDirs(recyclerView, viewHolder)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val item = adapter.getItemAt(position)
                if (item != null) {
                    viewModel.deleteRiwayat(item.riwayat.id)

                    Snackbar.make(binding.root, "'${item.comic.title}' dihapus dari riwayat", Snackbar.LENGTH_LONG)
                        .setAction("Urungkan") {
                            viewModel.reinsertRiwayat(item.riwayat)
                        }
                        .setActionTextColor(ContextCompat.getColor(requireContext(), R.color.mint_teal))
                        .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.bottom_navbar))
                        .show()
                } else {
                    adapter.notifyItemChanged(position)
                }
            }
        }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.listRiwayat)
    }

    private fun observeViewModel() {
        viewModel.riwayatList.observe(viewLifecycleOwner) { list ->
            currentList = list
            updateDynamicSourceChips(list)
            adapter.updateData(list)
            applyCurrentFilter()
            updateEmptyState()
        }
    }

    private fun setupSearchAndFilter() {
        binding.etSearchHistory.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s?.toString()?.trim().orEmpty()
                binding.btnClearSearch.visibility = if (currentQuery.isNotEmpty()) View.VISIBLE else View.GONE
                applyCurrentFilter()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchHistory.text?.clear()
            currentQuery = ""
            binding.btnClearSearch.visibility = View.GONE
            applyCurrentFilter()
        }

        binding.chipSemua.setOnClickListener {
            selectedSource = "Semua"
            applyCurrentFilter()
        }
    }

    private fun updateDynamicSourceChips(list: List<RiwayatDetail>) {
        val sources = list.mapNotNull { it.comic.location.takeIf { loc -> loc.isNotBlank() } }.distinct()
        val chipGroup = binding.chipGroupSources

        // Keep chipSemua, remove other dynamic chips to avoid duplicates
        val childCount = chipGroup.childCount
        for (i in childCount - 1 downTo 1) {
            chipGroup.removeViewAt(i)
        }

        val context = requireContext()
        for (source in sources) {
            val chip = Chip(context, null, com.google.android.material.R.style.Widget_Material3_Chip_Filter).apply {
                text = source
                isCheckable = true
                isChecked = (source.equals(selectedSource, ignoreCase = true))
                setChipBackgroundColorResource(R.color.background_slate_800)
                setTextColor(ContextCompat.getColor(context, R.color.white))
                textSize = 12f
                chipStrokeWidth = 1f
                setChipStrokeColorResource(R.color.surface_light)

                setOnClickListener {
                    selectedSource = source
                    applyCurrentFilter()
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun applyCurrentFilter() {
        adapter.filter(currentQuery, selectedSource)
        updateEmptyState()
    }

    private fun updateEmptyState() {
        val count = adapter.getFilteredItemCount()
        if (count == 0) {
            binding.listRiwayat.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE

            if (currentQuery.isNotBlank() || selectedSource != "Semua") {
                binding.tvEmptyTitle.text = "Komik Tidak Ditemukan"
                binding.tvEmptySubtitle.text = "Tidak ada riwayat baca yang sesuai dengan pencarian atau filter sumber ini."
                binding.btnExplore.visibility = View.GONE
            } else {
                binding.tvEmptyTitle.text = "Belum Ada Riwayat Baca"
                binding.tvEmptySubtitle.text = "Komik yang kamu baca akan otomatis tersimpan di sini agar mudah dilanjutkan kapan saja."
                binding.btnExplore.visibility = View.VISIBLE
            }
        } else {
            binding.listRiwayat.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
        }
    }

    private fun setupHeaderActions() {
        // Clear all history
        binding.btnClearAll.setOnClickListener {
            if (currentList.isEmpty()) {
                Toast.makeText(requireContext(), "Riwayat sudah kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialogCostum(
                context = requireContext(),
                title = "Bersihkan Semua Riwayat",
                message = "Apakah Anda yakin ingin menghapus semua riwayat bacaan? Daftar komik yang tersimpan di Pustaka tetap aman.",
                onPositive = { dialog ->
                    viewModel.deleteAllRiwayat()
                    dialog.dismiss()
                    Toast.makeText(requireContext(), "Seluruh riwayat berhasil dibersihkan", Toast.LENGTH_SHORT).show()
                },
                onNegative = { dialog ->
                    dialog.dismiss()
                }
            ).show()
        }

        // Empty state explore button -> Navigate to home
        binding.btnExplore.setOnClickListener {
            try {
                findNavController().navigate(R.id.homefragment)
            } catch (_: Exception) {
                // Fallback: switch via MainActivity
                (activity as? MainActivity)?.let { main ->
                    // Navigation will handle or user can tap bottom nav
                }
            }
        }
    }

    private fun showDeleteSingleConfirmation(item: RiwayatDetail) {
        AlertDialogCostum(
            context = requireContext(),
            title = "Hapus Riwayat",
            message = "Hapus '${item.comic.title}' dari riwayat baca?",
            onPositive = { dialog ->
                viewModel.deleteRiwayat(item.riwayat.id)
                dialog.dismiss()
            },
            onNegative = { dialog ->
                dialog.dismiss()
            }
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
