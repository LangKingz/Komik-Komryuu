package com.komik.komik.ui.pustaka

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.komik.komik.R
import com.komik.komik.databinding.BottomsheetFilterPustakaBinding
import com.komik.komik.databinding.FragmentPustakaBinding
import com.komik.komik.service.adapter.AdapterPustaka
import com.komik.komik.service.component.AlertDialogCostum
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.dao.LibraryDao
import com.komik.komik.ui.pages.login.LoginActivity

class PustakaFragment : Fragment() {

    private var _binding: FragmentPustakaBinding? = null
    val binding get() = _binding!!

    private lateinit var adapter: AdapterPustaka
    private lateinit var db: DatabaseRiwayats
    private lateinit var library: LibraryDao
    private lateinit var viewmodel: PustakaViewmodel

    private var isListMode: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPustakaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initDependencies()
        setupToolbar()
        setupRecyclerView()
        setupSearchAndFilters()
        setupBulkActionBar()
        observeViewModel()
    }

    private fun initDependencies() {
        db = DatabaseRiwayats.getDatabase(requireContext())
        library = db.libraryDao()
        viewmodel = ViewModelProvider(this, PustakaProvider(library))[PustakaViewmodel::class.java]

        val pref = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isSafe = pref.getBoolean("safemode", false)
        viewmodel.setSafeModeEnabled(isSafe)
    }

    private fun setupToolbar() {
        val activity = activity as? AppCompatActivity
        activity?.setSupportActionBar(binding.appbar)

        binding.appbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_downloads -> {
                    startActivity(Intent(requireContext(), com.komik.komik.ui.pages.download.DownloadManagerActivity::class.java))
                    true
                }
                R.id.sync_cloud -> {
                    viewmodel.syncLibraryToFirestore()
                    true
                }
                R.id.grid_layout -> {
                    toggleLayoutMode()
                    true
                }
                R.id.delete_all -> {
                    showDeleteAllConfirmation()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = AdapterPustaka(mutableListOf(), isListMode = false)
        binding.listPustaka.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.listPustaka.adapter = adapter

        adapter.onSelectionChangedListener = { count ->
            if (count > 0) {
                binding.bulkActionBar.visibility = View.VISIBLE
                binding.tvSelectedCount.text = "$count dipilih"
            } else {
                binding.bulkActionBar.visibility = View.GONE
            }
        }

        adapter.onItemDeletedListener = {
            updateBadgeCount()
        }

        binding.swipeRefresh.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.primary_button))
        binding.swipeRefresh.setOnRefreshListener {
            val pref = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            viewmodel.setSafeModeEnabled(pref.getBoolean("safemode", false))
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun toggleLayoutMode() {
        isListMode = !isListMode
        adapter.isListMode = isListMode

        if (isListMode) {
            binding.listPustaka.layoutManager = LinearLayoutManager(requireContext())
        } else {
            binding.listPustaka.layoutManager = GridLayoutManager(requireContext(), 3)
        }
        adapter.notifyDataSetChanged()
    }

    private fun setupSearchAndFilters() {
        binding.etSearchPustaka.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                viewmodel.setSearchQuery(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchPustaka.setText("")
        }

        binding.quickChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(binding.chipQuickReading.id) -> viewmodel.setFilterStatus(FilterStatus.READING)
                checkedIds.contains(binding.chipQuickCompleted.id) -> viewmodel.setFilterStatus(FilterStatus.COMPLETED)
                checkedIds.contains(binding.chipQuickPlan.id) -> viewmodel.setFilterStatus(FilterStatus.PLAN)
                else -> viewmodel.setFilterStatus(FilterStatus.ALL)
            }
        }

        binding.btnOpenFilter.setOnClickListener {
            showFilterBottomSheet()
        }
    }

    private fun showFilterBottomSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetBinding = BottomsheetFilterPustakaBinding.inflate(layoutInflater)
        bottomSheet.setContentView(sheetBinding.root)

        when (viewmodel.currentStatusFilter) {
            FilterStatus.ALL -> sheetBinding.chipSemua.isChecked = true
            FilterStatus.READING -> sheetBinding.chipSedangDibaca.isChecked = true
            FilterStatus.COMPLETED -> sheetBinding.chipSelesai.isChecked = true
            FilterStatus.PLAN -> sheetBinding.chipRencanaBaca.isChecked = true
        }

        when (viewmodel.currentSortOrder) {
            SortOrder.NEWEST -> sheetBinding.chipSortTerbaru.isChecked = true
            SortOrder.TITLE_AZ -> sheetBinding.chipSortAbjad.isChecked = true
            SortOrder.LAST_READ -> sheetBinding.chipSortTerakhirDibaca.isChecked = true
        }

        sheetBinding.btnApplyFilter.setOnClickListener {
            val status = when {
                sheetBinding.chipSedangDibaca.isChecked -> FilterStatus.READING
                sheetBinding.chipSelesai.isChecked -> FilterStatus.COMPLETED
                sheetBinding.chipRencanaBaca.isChecked -> FilterStatus.PLAN
                else -> FilterStatus.ALL
            }

            val sort = when {
                sheetBinding.chipSortAbjad.isChecked -> SortOrder.TITLE_AZ
                sheetBinding.chipSortTerakhirDibaca.isChecked -> SortOrder.LAST_READ
                else -> SortOrder.NEWEST
            }

            viewmodel.setFilterStatus(status)
            viewmodel.setSortOrder(sort)

            when (status) {
                FilterStatus.ALL -> binding.chipQuickSemua.isChecked = true
                FilterStatus.READING -> binding.chipQuickReading.isChecked = true
                FilterStatus.COMPLETED -> binding.chipQuickCompleted.isChecked = true
                FilterStatus.PLAN -> binding.chipQuickPlan.isChecked = true
            }

            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }

    private fun setupBulkActionBar() {
        binding.btnDeleteSelected.setOnClickListener {
            val selectedCount = adapter.selectedIds.size
            if (selectedCount > 0) {
                AlertDialogCostum(
                    requireContext(),
                    "Hapus Item Terpilih",
                    "Apakah Anda yakin ingin menghapus $selectedCount komik terpilih dari pustaka?",
                    onPositive = { dialog ->
                        adapter.deleteSelectedItems(requireContext())
                        binding.bulkActionBar.visibility = View.GONE
                        dialog.dismiss()
                        Toast.makeText(requireContext(), "$selectedCount komik berhasil dihapus", Toast.LENGTH_SHORT).show()
                    },
                    onNegative = { dialog ->
                        dialog.dismiss()
                    }
                ).show()
            }
        }

        binding.btnCancelBulk.setOnClickListener {
            adapter.isSelectionMode = false
            binding.bulkActionBar.visibility = View.GONE
        }
    }

    private fun showDeleteAllConfirmation() {
        AlertDialogCostum(
            requireContext(),
            "Hapus Semua Pustaka",
            "Apakah Anda yakin ingin menghapus SEMUA komik dari pustaka lokal?",
            onPositive = { dialog ->
                viewmodel.deleteAll()
                dialog.dismiss()
                Toast.makeText(requireContext(), "Semua komik di pustaka berhasil dihapus", Toast.LENGTH_SHORT).show()
            },
            onNegative = { dialog ->
                dialog.dismiss()
            }
        ).show()
    }

    private fun observeViewModel() {
        viewmodel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.swipeRefresh.isRefreshing = isLoading
        }

        viewmodel.filteredData.observe(viewLifecycleOwner) { data ->
            if (data.isNullOrEmpty()) {
                binding.listPustaka.visibility = View.GONE
                binding.placeholder.visibility = View.VISIBLE
                if (viewmodel.currentSearchQuery.isNotEmpty()) {
                    binding.tvEmptyTitle.text = "Hasil Tidak Ditemukan"
                    binding.tvEmptySubtitle.text = "Tidak ada komik yang cocok dengan \"${viewmodel.currentSearchQuery}\""
                } else {
                    binding.tvEmptyTitle.text = "Pustaka Masih Kosong"
                    binding.tvEmptySubtitle.text = "Tandai komik favorit dari daftar ekstensi untuk menyimpannya di sini."
                }
                binding.appbar.subtitle = "0 Komik Tersimpan"
            } else {
                binding.placeholder.visibility = View.GONE
                binding.listPustaka.visibility = View.VISIBLE
                adapter.setData(data)
                binding.appbar.subtitle = "${data.size} Komik Tersimpan"
            }
        }

        viewmodel.syncStatus.observe(viewLifecycleOwner) { result ->
            when (result) {
                is PustakaViewmodel.SyncResult.Success -> {
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.mint_teal))
                        .setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                        .show()
                }
                is PustakaViewmodel.SyncResult.Error -> {
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG)
                        .setAction("Login") {
                            val intent = Intent(requireContext(), LoginActivity::class.java)
                            startActivity(intent)
                        }
                        .show()
                }
            }
        }
    }

    private fun updateBadgeCount() {
        // badge updates dynamically via Room
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}