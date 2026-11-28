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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.komik.komik.R
import com.komik.komik.databinding.BottomsheetFilterPustakaBinding
import com.komik.komik.databinding.FragmentPustakaBinding
import com.komik.komik.service.adapter.AdapterPustaka
import com.komik.komik.service.component.AlertDialogCostum
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.dao.LibraryDao
import com.komik.komik.service.utils.PreferenceManager
import com.komik.komik.service.utils.calculateGridSpanCount
import com.komik.komik.service.utils.isTablet
import com.komik.komik.ui.pages.login.LoginActivity
import com.komik.komik.ui.pages.search.SearchActivity

class PustakaFragment : Fragment() {

    private var _binding: FragmentPustakaBinding? = null
    val binding get() = _binding!!

    private lateinit var adapter: AdapterPustaka
    private lateinit var db: DatabaseRiwayats
    private lateinit var library: LibraryDao
    private lateinit var viewmodel: PustakaViewmodel
    private lateinit var prefManager: PreferenceManager

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
        setupControls()
        setupSearchAndFilters()
        setupBulkActionBar()
        setupEmptyState()
        observeViewModel()
    }

    private fun initDependencies() {
        prefManager = PreferenceManager.getInstance(requireContext())
        db = DatabaseRiwayats.getDatabase(requireContext())
        library = db.libraryDao()
        viewmodel = ViewModelProvider(this, PustakaProvider(library, prefManager))[PustakaViewmodel::class.java]
        viewmodel.prefManager = prefManager

        val isSafe = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE).getBoolean("safemode", false)
        viewmodel.setSafeModeEnabled(isSafe)
    }

    private fun setupToolbar() {
        val activity = activity as? AppCompatActivity
        activity?.setSupportActionBar(binding.appbar)

        binding.btnChapterUpdates.setOnClickListener {
            startActivity(Intent(requireContext(), com.komik.komik.ui.pages.updates.ChapterUpdatesActivity::class.java))
        }

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
                    toggleListGridMode()
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
        val isTablet = requireContext().isTablet()
        val optimalSpan = requireContext().calculateGridSpanCount()
        val initialSpan = if (isTablet) {
            optimalSpan
        } else {
            if (prefManager.pustakaGridSpan in 2..3) prefManager.pustakaGridSpan else 3
        }
        adapter = AdapterPustaka(mutableListOf(), isListMode = false, spanCount = initialSpan)
        binding.listPustaka.layoutManager = GridLayoutManager(requireContext(), initialSpan)
        binding.listPustaka.adapter = adapter
        updateGridToggleButtonText(initialSpan)

        adapter.onSelectionChangedListener = { count ->
            if (count > 0) {
                binding.bulkActionBar.visibility = View.VISIBLE
                binding.tvSelectedCount.text = "$count dipilih"
            } else {
                binding.bulkActionBar.visibility = View.GONE
            }
        }

        adapter.onItemDeletedListener = {
            viewmodel.refreshFilters()
        }

        binding.swipeRefresh.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.primary_button))
        binding.swipeRefresh.setOnRefreshListener {
            val isSafe = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE).getBoolean("safemode", false)
            viewmodel.setSafeModeEnabled(isSafe)
            viewmodel.refreshFilters()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setupControls() {
        binding.btnToggleGrid.setOnClickListener {
            if (isListMode) {
                isListMode = false
                adapter.isListMode = false
            }
            val isTablet = requireContext().isTablet()
            val optimal = requireContext().calculateGridSpanCount()
            val currentSpan = adapter.spanCount
            val newSpan = if (isTablet) {
                if (currentSpan == optimal) (optimal - 1).coerceAtLeast(2) else optimal
            } else {
                if (currentSpan == 2) 3 else 2
            }
            prefManager.pustakaGridSpan = newSpan
            adapter.spanCount = newSpan

            binding.listPustaka.layoutManager = GridLayoutManager(requireContext(), newSpan)
            adapter.notifyItemRangeChanged(0, adapter.itemCount)
            updateGridToggleButtonText(newSpan)
        }

        binding.btnSortPustaka.setOnClickListener {
            showSortDialog()
        }

        binding.btnOpenFilter.setOnClickListener {
            showFilterBottomSheet()
        }
    }

    private fun updateGridToggleButtonText(span: Int) {
        binding.tvGridLabel.text = "Grid $span"
    }

    private fun toggleListGridMode() {
        isListMode = !isListMode
        adapter.isListMode = isListMode

        if (isListMode) {
            binding.listPustaka.layoutManager = LinearLayoutManager(requireContext())
        } else {
            val isTablet = requireContext().isTablet()
            val span = if (isTablet) {
                requireContext().calculateGridSpanCount()
            } else {
                if (prefManager.pustakaGridSpan in 2..3) prefManager.pustakaGridSpan else 3
            }
            binding.listPustaka.layoutManager = GridLayoutManager(requireContext(), span)
        }
        adapter.notifyDataSetChanged()
    }

    private fun showSortDialog() {
        val sortLabels = arrayOf(
            "Terbaru Ditambahkan",
            "Terakhir Dibaca",
            "Judul (A – Z)",
            "Judul (Z – A)"
        )
        val sortEnums = arrayOf(
            SortOrder.NEWEST,
            SortOrder.LAST_READ,
            SortOrder.TITLE_AZ,
            SortOrder.TITLE_ZA
        )

        val selectedIndex = when (viewmodel.currentSortOrder) {
            SortOrder.NEWEST -> 0
            SortOrder.LAST_READ -> 1
            SortOrder.TITLE_AZ -> 2
            SortOrder.TITLE_ZA -> 3
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Urutkan Pustaka")
            .setSingleChoiceItems(sortLabels, selectedIndex) { dialog, which ->
                val chosenSort = sortEnums[which]
                viewmodel.setSortOrder(chosenSort)
                binding.tvSortLabel.text = when (chosenSort) {
                    SortOrder.NEWEST -> "Terbaru"
                    SortOrder.LAST_READ -> "Terakhir Baca"
                    SortOrder.TITLE_AZ -> "A – Z"
                    SortOrder.TITLE_ZA -> "Z – A"
                }
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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
                checkedIds.contains(binding.chipQuickPlan.id) -> viewmodel.setFilterStatus(FilterStatus.PLAN)
                checkedIds.contains(binding.chipQuickCompleted.id) -> viewmodel.setFilterStatus(FilterStatus.COMPLETED)
                checkedIds.contains(binding.chipQuickOnHold.id) -> viewmodel.setFilterStatus(FilterStatus.ON_HOLD)
                else -> viewmodel.setFilterStatus(FilterStatus.ALL)
            }
        }
    }

    private fun showFilterBottomSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetBinding = BottomsheetFilterPustakaBinding.inflate(layoutInflater)
        bottomSheet.setContentView(sheetBinding.root)

        when (viewmodel.currentStatusFilter) {
            FilterStatus.ALL -> sheetBinding.chipSemua.isChecked = true
            FilterStatus.READING -> sheetBinding.chipSedangDibaca.isChecked = true
            FilterStatus.PLAN -> sheetBinding.chipRencanaBaca.isChecked = true
            FilterStatus.COMPLETED -> sheetBinding.chipSelesai.isChecked = true
            FilterStatus.ON_HOLD -> sheetBinding.chipDitunda.isChecked = true
        }

        when (viewmodel.currentSortOrder) {
            SortOrder.NEWEST -> sheetBinding.chipSortTerbaru.isChecked = true
            SortOrder.LAST_READ -> sheetBinding.chipSortTerakhirDibaca.isChecked = true
            SortOrder.TITLE_AZ -> sheetBinding.chipSortAbjad.isChecked = true
            SortOrder.TITLE_ZA -> sheetBinding.chipSortAbjadDesc.isChecked = true
        }

        sheetBinding.btnApplyFilter.setOnClickListener {
            val status = when {
                sheetBinding.chipSedangDibaca.isChecked -> FilterStatus.READING
                sheetBinding.chipRencanaBaca.isChecked -> FilterStatus.PLAN
                sheetBinding.chipSelesai.isChecked -> FilterStatus.COMPLETED
                sheetBinding.chipDitunda.isChecked -> FilterStatus.ON_HOLD
                else -> FilterStatus.ALL
            }

            val sort = when {
                sheetBinding.chipSortTerakhirDibaca.isChecked -> SortOrder.LAST_READ
                sheetBinding.chipSortAbjad.isChecked -> SortOrder.TITLE_AZ
                sheetBinding.chipSortAbjadDesc.isChecked -> SortOrder.TITLE_ZA
                else -> SortOrder.NEWEST
            }

            viewmodel.setFilterStatus(status)
            viewmodel.setSortOrder(sort)

            when (status) {
                FilterStatus.ALL -> binding.chipQuickSemua.isChecked = true
                FilterStatus.READING -> binding.chipQuickReading.isChecked = true
                FilterStatus.PLAN -> binding.chipQuickPlan.isChecked = true
                FilterStatus.COMPLETED -> binding.chipQuickCompleted.isChecked = true
                FilterStatus.ON_HOLD -> binding.chipQuickOnHold.isChecked = true
            }

            binding.tvSortLabel.text = when (sort) {
                SortOrder.NEWEST -> "Terbaru"
                SortOrder.LAST_READ -> "Terakhir Baca"
                SortOrder.TITLE_AZ -> "A – Z"
                SortOrder.TITLE_ZA -> "Z – A"
            }

            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }

    private fun setupBulkActionBar() {
        binding.btnMoveShelf.setOnClickListener {
            val selectedCount = adapter.selectedIds.size
            if (selectedCount == 0) return@setOnClickListener

            val shelfOptions = arrayOf(
                "Semua (Default)",
                "Sedang Dibaca",
                "Rencana Baca",
                "Tamat",
                "Ditunda"
            )
            val shelfKeys = arrayOf("all", "reading", "plan", "completed", "on_hold")

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Pindahkan $selectedCount Komik ke Rak")
                .setItems(shelfOptions) { dialog, which ->
                    val chosenShelfKey = shelfKeys[which]
                    val chosenShelfName = shelfOptions[which]
                    prefManager.setBatchComicShelfStatus(adapter.selectedIds, chosenShelfKey)
                    Toast.makeText(requireContext(), "$selectedCount komik dipindahkan ke rak $chosenShelfName", Toast.LENGTH_SHORT).show()
                    adapter.isSelectionMode = false
                    binding.bulkActionBar.visibility = View.GONE
                    viewmodel.refreshFilters()
                    dialog.dismiss()
                }
                .setNegativeButton("Batal") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

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

    private fun setupEmptyState() {
        binding.btnExplore.setOnClickListener {
            val mainActivity = activity as? com.komik.komik.MainActivity
            if (mainActivity != null) {
                mainActivity.selectTab(R.id.homefragment)
            } else {
                val intent = Intent(requireContext(), SearchActivity::class.java)
                startActivity(intent)
            }
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
            val count = data?.size ?: 0
            binding.tvTotalBadge.text = "$count Komik"

            if (data.isNullOrEmpty()) {
                binding.listPustaka.visibility = View.GONE
                binding.placeholder.visibility = View.VISIBLE
                if (viewmodel.currentSearchQuery.isNotEmpty()) {
                    binding.tvEmptyTitle.text = "Hasil Tidak Ditemukan"
                    binding.tvEmptySubtitle.text = "Tidak ada komik yang cocok dengan \"${viewmodel.currentSearchQuery}\""
                    binding.btnExplore.visibility = View.GONE
                } else {
                    binding.tvEmptyTitle.text = "Pustaka Masih Kosong"
                    binding.tvEmptySubtitle.text = "Tandai komik favorit dari daftar ekstensi untuk menyimpannya di sini."
                    binding.btnExplore.visibility = View.VISIBLE
                }
            } else {
                binding.placeholder.visibility = View.GONE
                binding.listPustaka.visibility = View.VISIBLE
                adapter.setData(data)
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

        // Pantau badge unread update chapter baru
        viewLifecycleOwner.lifecycleScope.launch {
            db.chapterUpdateDao().getUnreadCount().collectLatest { unreadCount ->
                if (_binding != null) {
                    if (unreadCount > 0) {
                        binding.tvUpdateBadge.visibility = View.VISIBLE
                        binding.tvUpdateBadge.text = if (unreadCount > 99) "99+" else unreadCount.toString()
                    } else {
                        binding.tvUpdateBadge.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
