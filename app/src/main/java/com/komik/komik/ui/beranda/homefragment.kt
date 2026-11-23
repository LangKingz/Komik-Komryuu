package com.komik.komik.ui.beranda

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.komik.komik.MainActivity
import com.komik.komik.R
import com.komik.komik.databinding.FragmentHomefragmentBinding
import com.komik.komik.service.adapter.AdapterQuickResume
import com.komik.komik.service.adapter.AdapterSource
import com.komik.komik.service.api.model.DataItems
import com.komik.komik.service.component.BottomSheetReport
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.dao.riwayatRelationDao
import com.komik.komik.service.utils.AppConfigHelper
import com.komik.komik.service.utils.PreferenceManager
import com.komik.komik.ui.pages.login.LoginActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class homefragment : Fragment() {

    private var _binding: FragmentHomefragmentBinding? = null
    val binding get() = _binding!!

    private lateinit var viewmodel: Homeviewmodel
    private lateinit var dbUtama: DatabaseRiwayats
    private lateinit var riwayatdao: riwayatRelationDao

    private lateinit var adapterSource: AdapterSource
    private lateinit var adapterQuickResume: AdapterQuickResume

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomefragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewmodel = ViewModelProvider(this)[Homeviewmodel::class.java]
        dbUtama = DatabaseRiwayats.getDatabase(requireContext())
        riwayatdao = dbUtama.riwayatdao()

        setupHeader()
        setupBroadcastBanner()
        setupRecyclerViews()
        setupViewModeToggle()
        setupEcosystemStatusCard()
        setupSwipeRefresh()
        observeViewModel()
        loadQuickResume()

        val pref = PreferenceManager.getInstance(requireContext())
        val isPrivileged = pref.userRole.equals("admin", ignoreCase = true) ||
                pref.userRole.equals("moderator", ignoreCase = true)
        if (viewmodel.source.value == null) {
            viewmodel.loadHomeData(includeInactive = isPrivileged)
        }

        binding.refresh.setOnClickListener {
            refreshHomeContent()
        }

        binding.btnRetryLoadSource.setOnClickListener {
            refreshHomeContent()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshHome.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), R.color.primary_button)
        )
        binding.swipeRefreshHome.setOnRefreshListener {
            refreshHomeContent()
        }
    }

    private fun refreshHomeContent() {
        binding.layoutErrorSource.visibility = View.GONE
        adapterSource.refreshPings()
        val pref = PreferenceManager.getInstance(requireContext())
        val isPrivileged = pref.userRole.equals("admin", ignoreCase = true) ||
                pref.userRole.equals("moderator", ignoreCase = true)
        viewmodel.loadHomeData(includeInactive = isPrivileged)
        setupBroadcastBanner()
        updateEcosystemStatus(viewmodel.source.value)
    }

    // ----------------------------------------------------
    // 1. Dynamic Header (User greeting & Server Status)
    // ----------------------------------------------------
    private fun setupHeader() {
        val user = FirebaseAuth.getInstance().currentUser
        val timeGreeting = getTimeGreeting()

        if (user != null) {
            val name = user.displayName?.split(" ")?.firstOrNull() ?: "Pengguna"
            binding.greetings.text = "$timeGreeting, $name!"
            binding.tvSubGreeting.text = "Lanjut baca manhwa favoritmu"

            if (user.photoUrl != null) {
                Glide.with(this)
                    .load(user.photoUrl)
                    .circleCrop()
                    .error(R.drawable.logo_komryu_rb)
                    .into(binding.ivUserAvatar)
            } else {
                binding.ivUserAvatar.setImageResource(R.drawable.logo_komryu_rb)
            }
        } else {
            val pref = requireContext().getSharedPreferences(LoginActivity.PREF_USER, Context.MODE_PRIVATE)
            val isGuest = pref.getBoolean(LoginActivity.KEY_IS_GUEST, false)
            val label = if (isGuest) "Tamu" else "Pengguna"
            binding.greetings.text = "$timeGreeting, $label!"
            binding.tvSubGreeting.text = "Jelajahi ribuan komik di Komryuu.X"
            binding.ivUserAvatar.setImageResource(R.drawable.logo_komryu_rb)
        }

        binding.btnHeaderReportInfo.setOnClickListener {
            showHelpReportDialog()
        }
    }

    private fun getTimeGreeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 4..11 -> "Selamat Pagi"
            in 12..15 -> "Selamat Siang"
            in 16..18 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
    }

    // ----------------------------------------------------
    // 2. Ecosystem Status Card & Help Report
    // ----------------------------------------------------
    private fun setupEcosystemStatusCard() {
        binding.btnEcosystemAction.setOnClickListener {
            showHelpReportDialog()
        }
        binding.cardEcosystemStatus.setOnClickListener {
            showHelpReportDialog()
        }
        updateEcosystemStatus(viewmodel.source.value)
    }

    private fun updateEcosystemStatus(sources: List<DataItems>?) {
        if (_binding == null) return
        val pref = PreferenceManager.getInstance(requireContext())

        // Active extensions count
        if (!sources.isNullOrEmpty()) {
            val isPrivileged = pref.userRole.equals("admin", ignoreCase = true) ||
                    pref.userRole.equals("moderator", ignoreCase = true)
            val activeCount = sources.count { it.isActive != false }
            val inactiveCount = sources.size - activeCount

            if (isPrivileged && inactiveCount > 0) {
                binding.tvEcosystemTitle.text = "$activeCount Aktif • $inactiveCount Maintenance (Admin Test)"
            } else {
                binding.tvEcosystemTitle.text = "$activeCount Sumber Ekstensi Aktif • Siap Membaca"
            }
        } else {
            binding.tvEcosystemTitle.text = "Memuat status ekstensi..."
        }

        // Cloud sync status
        val lastSync = pref.lastCloudSyncMillis
        if (lastSync > 0L) {
            val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            binding.tvEcosystemSubtitle.text = "Cloud Sync: ${sdf.format(Date(lastSync))}"
        } else {
            binding.tvEcosystemSubtitle.text = "Cloud Sync: Belum disinkronkan"
        }
    }

    private fun showHelpReportDialog() {
        try {
            val reportSheet = BottomSheetReport.newInstance(
                sourceName = "Komryuu Ecosystem",
                comicTitle = "Pusat Bantuan & Laporan",
                chapterTitle = null,
                targetUrl = "app://home",
                defaultType = "other"
            )
            reportSheet.show(childFragmentManager, BottomSheetReport.TAG)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal membuka pusat bantuan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ----------------------------------------------------
    // 3. Setup RecyclerViews (Quick Resume & Source Extensions)
    // ----------------------------------------------------
    private fun setupRecyclerViews() {
        // Quick Resume
        adapterQuickResume = AdapterQuickResume(mutableListOf())
        binding.rvQuickResume.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvQuickResume.adapter = adapterQuickResume

        // Source Extensions (Grid / List dynamic span)
        adapterSource = AdapterSource(mutableListOf())
        adapterSource.initPinnedCache(requireContext())

        val pref = PreferenceManager.getInstance(requireContext())
        val currentMode = pref.sourceViewMode
        val initialSpan = if (currentMode == "list") 1 else 2

        val sourceLayoutManager = GridLayoutManager(requireContext(), initialSpan).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (adapterSource.getItemViewType(position)) {
                        AdapterSource.VIEW_TYPE_HEADER -> this@apply.spanCount
                        else -> 1
                    }
                }
            }
        }
        binding.listSource.layoutManager = sourceLayoutManager
        binding.listSource.adapter = adapterSource

        applySourceViewMode(currentMode)
    }

    // ----------------------------------------------------
    // 4. View Mode Toggle (Grid ↔ List)
    // ----------------------------------------------------
    private fun setupViewModeToggle() {
        binding.btnToggleSourceViewMode.setOnClickListener {
            val pref = PreferenceManager.getInstance(requireContext())
            val newMode = if (pref.sourceViewMode == "list") "grid" else "list"
            pref.sourceViewMode = newMode
            applySourceViewMode(newMode)
        }
    }

    private fun applySourceViewMode(mode: String) {
        val isList = mode.equals("list", ignoreCase = true)
        val spanCount = if (isList) 1 else 2

        (binding.listSource.layoutManager as? GridLayoutManager)?.spanCount = spanCount
        adapterSource.switchViewMode(if (isList) "list" else "grid")

        if (isList) {
            binding.ivViewModeIcon.setImageResource(R.drawable.ic_view_list)
            binding.tvViewModeLabel.text = "List"
        } else {
            binding.ivViewModeIcon.setImageResource(R.drawable.squares_four)
            binding.tvViewModeLabel.text = "Grid"
        }
    }

    // ----------------------------------------------------
    // 5. Observe ViewModel
    // ----------------------------------------------------
    private fun observeViewModel() {
        // Server Status
        viewmodel.isServerOnline.observe(viewLifecycleOwner) { isOnline ->
            if (isOnline) {
                binding.dotServerStatus.setBackgroundResource(R.drawable.badge_status_online)
                binding.tvServerStatus.text = "Online"
                binding.tvServerStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_emerald_400))
            } else {
                binding.dotServerStatus.setBackgroundResource(R.drawable.badge_status_offline)
                binding.tvServerStatus.text = "Offline"
                binding.tvServerStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.btn_trek1))
            }
        }

        // Source Extensions
        viewmodel.source.observe(viewLifecycleOwner) { sources ->
            updateEcosystemStatus(sources)
            if (!sources.isNullOrEmpty()) {
                binding.layoutErrorSource.visibility = View.GONE
                binding.listSource.visibility = View.VISIBLE
                adapterSource.initPinnedCache(requireContext())

                val pref = PreferenceManager.getInstance(requireContext())
                val isPrivileged = pref.userRole.equals("admin", ignoreCase = true) ||
                        pref.userRole.equals("moderator", ignoreCase = true)
                val filteredSources = if (isPrivileged) sources else sources.filter { it.isActive != false }

                adapterSource.updateData(filteredSources)
            } else if (viewmodel.loading.value != true) {
                binding.layoutErrorSource.visibility = View.VISIBLE
                binding.listSource.visibility = View.GONE
            }
        }

        // Loading
        viewmodel.loading.observe(viewLifecycleOwner) { loading ->
            binding.loading.visibility = if (loading) View.VISIBLE else View.GONE
            if (!loading) {
                binding.swipeRefreshHome.isRefreshing = false
                if (viewmodel.source.value.isNullOrEmpty()) {
                    binding.layoutErrorSource.visibility = View.VISIBLE
                    binding.listSource.visibility = View.GONE
                } else {
                    binding.layoutErrorSource.visibility = View.GONE
                    binding.listSource.visibility = View.VISIBLE
                }
            }
        }

        // Error
        viewmodel.error.observe(viewLifecycleOwner) { error ->
            binding.layoutRefresh.visibility = if (error != null) View.VISIBLE else View.GONE
            if (error != null) {
                binding.swipeRefreshHome.isRefreshing = false
                if (viewmodel.source.value.isNullOrEmpty()) {
                    binding.layoutErrorSource.visibility = View.VISIBLE
                    binding.listSource.visibility = View.GONE
                }
            }
        }
    }

    // ----------------------------------------------------
    // 6. Quick Resume ("Lanjut Baca") from Room Database
    // ----------------------------------------------------
    private fun loadQuickResume() {
        riwayatdao.getRiwayatDetail().observe(viewLifecycleOwner) { riwayatList ->
            if (!riwayatList.isNullOrEmpty()) {
                binding.sectionQuickResume.visibility = View.VISIBLE
                adapterQuickResume.updateData(riwayatList.take(8))
            } else {
                binding.sectionQuickResume.visibility = View.GONE
            }
        }
    }

    // ----------------------------------------------------
    // Broadcast Banner Dinamis dari Config Komryuu.x
    // ----------------------------------------------------
    private fun setupBroadcastBanner() {
        viewLifecycleOwner.lifecycleScope.launch {
            val config = AppConfigHelper.cachedConfig ?: AppConfigHelper.fetchConfig()
            val banner = config?.activeBanner
            if (_binding == null) return@launch

            if (banner != null && banner.isActive && !AppConfigHelper.isBannerDismissed(requireContext(), banner)) {
                binding.cardBroadcastBanner.visibility = View.VISIBLE
                binding.tvBannerTitle.text = banner.title.ifEmpty { "Pengumuman" }
                binding.tvBannerMessage.text = banner.message

                val strokeColor: Int
                val iconBgColor: Int
                val iconTint: Int
                val iconRes: Int

                when (banner.type.lowercase()) {
                    "urgent" -> {
                        strokeColor = android.graphics.Color.parseColor("#EF4444")
                        iconBgColor = android.graphics.Color.parseColor("#3B1824")
                        iconTint = android.graphics.Color.parseColor("#F87171")
                        iconRes = R.drawable.ic_fire_streak
                    }
                    "warning" -> {
                        strokeColor = android.graphics.Color.parseColor("#F59E0B")
                        iconBgColor = android.graphics.Color.parseColor("#3D2612")
                        iconTint = android.graphics.Color.parseColor("#FBBF24")
                        iconRes = R.drawable.bell_ringing
                    }
                    "update" -> {
                        strokeColor = android.graphics.Color.parseColor("#10B981")
                        iconBgColor = android.graphics.Color.parseColor("#1A382B")
                        iconTint = android.graphics.Color.parseColor("#34D399")
                        iconRes = R.drawable.baseline_browser_updated_24
                    }
                    else -> { // info
                        strokeColor = android.graphics.Color.parseColor("#4F46E5")
                        iconBgColor = android.graphics.Color.parseColor("#264F46E5")
                        iconTint = android.graphics.Color.parseColor("#818CF8")
                        iconRes = R.drawable.bell_ringing
                    }
                }

                binding.cardBroadcastBanner.strokeColor = strokeColor
                binding.bannerIconFrame.backgroundTintList = android.content.res.ColorStateList.valueOf(iconBgColor)
                binding.ivBannerIcon.setImageResource(iconRes)
                binding.ivBannerIcon.setColorFilter(iconTint)

                binding.btnCloseBanner.setOnClickListener {
                    AppConfigHelper.dismissBanner(requireContext(), banner)
                    binding.cardBroadcastBanner.visibility = View.GONE
                }
            } else {
                binding.cardBroadcastBanner.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.showBottomNav()
        updateEcosystemStatus(viewmodel.source.value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
