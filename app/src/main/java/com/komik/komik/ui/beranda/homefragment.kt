package com.komik.komik.ui.beranda

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.komik.komik.MainActivity
import com.komik.komik.R
import com.komik.komik.databinding.FragmentHomefragmentBinding
import com.komik.komik.service.adapter.AdapterCorousel
import com.komik.komik.service.adapter.AdapterGenre
import com.komik.komik.service.adapter.AdapterManhwa
import com.komik.komik.service.adapter.AdapterQuickResume
import com.komik.komik.service.adapter.AdapterSource
import com.komik.komik.service.api.model.DataItems
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.dao.riwayatRelationDao
import com.komik.komik.service.parser.model.getListUpdate
import com.komik.komik.service.utils.AppConfigHelper
import com.komik.komik.service.utils.PreferenceManager
import com.komik.komik.ui.pages.login.LoginActivity
import kotlinx.coroutines.launch
import java.util.Calendar

class homefragment : Fragment() {

    private var _binding: FragmentHomefragmentBinding? = null
    val binding get() = _binding!!

    private lateinit var viewmodel: Homeviewmodel
    private lateinit var dbUtama: DatabaseRiwayats
    private lateinit var riwayatdao: riwayatRelationDao

    private lateinit var adapterSource: AdapterSource
    private lateinit var adapterLatest: AdapterManhwa
    private lateinit var adapterPopular: AdapterManhwa
    private lateinit var adapterQuickResume: AdapterQuickResume

    private val carouselHandler = Handler(Looper.getMainLooper())
    private var carouselRunnable: Runnable? = null
    private var carouselCount = 0

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
        setupScrollBehavior()
        setupSwipeRefresh()
        observeViewModel()
        loadQuickResume()

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
        viewmodel.loadHomeData()
        setupBroadcastBanner()
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

        binding.btnQuickSearch.setOnClickListener {
            try {
                findNavController().navigate(R.id.searchFragment)
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Cari komik...", Toast.LENGTH_SHORT).show()
            }
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
    // 2. Setup RecyclerViews & Hero Carousel
    // ----------------------------------------------------
    private fun setupRecyclerViews() {
        // Quick Resume
        adapterQuickResume = AdapterQuickResume(mutableListOf())
        binding.rvQuickResume.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvQuickResume.adapter = adapterQuickResume

        // Genres
        binding.rvGenres.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Latest Comics (3 Columns Grid)
        adapterLatest = AdapterManhwa(mutableListOf())
        binding.rvLatestComics.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvLatestComics.adapter = adapterLatest

        // Popular Comics (Horizontal)
        adapterPopular = AdapterManhwa(mutableListOf())
        binding.rvPopularComics.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvPopularComics.adapter = adapterPopular

        // Source Extensions (2 Columns with Header span)
        adapterSource = AdapterSource(mutableListOf())
        adapterSource.initPinnedCache(requireContext())
        val spanCount = 2
        val sourceLayoutManager = GridLayoutManager(requireContext(), spanCount).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (adapterSource.getItemViewType(position)) {
                        AdapterSource.VIEW_TYPE_HEADER -> spanCount
                        else -> 1
                    }
                }
            }
        }
        binding.listSource.layoutManager = sourceLayoutManager
        binding.listSource.adapter = adapterSource
    }

    // ----------------------------------------------------
    // 3. Scroll Behavior
    // ----------------------------------------------------
    private fun setupScrollBehavior() {
        // Bottom nav is docked to bottom; keeping stable view bounds
    }

    // ----------------------------------------------------
    // 4. Observe ViewModel
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

        // Hero Carousel
        viewmodel.carousel.observe(viewLifecycleOwner) { items ->
            if (!items.isNullOrEmpty()) {
                carouselCount = items.size
                val carouselAdapter = AdapterCorousel(items)
                binding.vpCarousel.adapter = carouselAdapter
                setupCarouselDots(carouselCount, 0)

                binding.vpCarousel.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        setupCarouselDots(carouselCount, position)
                    }
                })

                startCarouselAutoSlide()
            }
        }

        // Genres
        viewmodel.genres.observe(viewLifecycleOwner) { genres ->
            binding.rvGenres.adapter = AdapterGenre(genres) { genre ->
                Toast.makeText(requireContext(), "Genre: ${genre.genre}", Toast.LENGTH_SHORT).show()
            }
        }

        // Latest Comics
        viewmodel.latestComics.observe(viewLifecycleOwner) { comics ->
            if (comics.isNullOrEmpty()) {
                binding.sectionLatest.visibility = View.GONE
            } else {
                binding.sectionLatest.visibility = View.VISIBLE
                adapterLatest.updateData(comics)
            }
        }

        // Popular Comics
        viewmodel.popularComics.observe(viewLifecycleOwner) { comics ->
            if (comics.isNullOrEmpty()) {
                binding.sectionPopular.visibility = View.GONE
            } else {
                binding.sectionPopular.visibility = View.VISIBLE
                adapterPopular.updateData(comics)
            }
        }

        // Source Extensions (Filter isActive == true unless Admin/Moderator)
        viewmodel.source.observe(viewLifecycleOwner) { sources ->
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
    // 5. Quick Resume ("Lanjut Baca") from Room Database
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

    // ----------------------------------------------------
    // 6. Carousel Auto-slide & Dots
    // ----------------------------------------------------
    private fun setupCarouselDots(count: Int, selectedPosition: Int) {
        binding.layoutDots.removeAllViews()
        for (i in 0 until count) {
            val dot = View(requireContext()).apply {
                val isSelected = (i == selectedPosition)
                val widthDp = if (isSelected) 22 else 7
                val params = LinearLayout.LayoutParams(
                    (widthDp * resources.displayMetrics.density).toInt(),
                    (6 * resources.displayMetrics.density).toInt()
                ).apply {
                    marginEnd = (5 * resources.displayMetrics.density).toInt()
                }
                layoutParams = params
                background = ContextCompat.getDrawable(
                    requireContext(),
                    if (isSelected) R.drawable.badge_last_read else R.drawable.bg_read_btn
                )
            }
            binding.layoutDots.addView(dot)
        }
    }

    private fun startCarouselAutoSlide() {
        carouselRunnable?.let { carouselHandler.removeCallbacks(it) }
        carouselRunnable = object : Runnable {
            override fun run() {
                if (_binding != null && carouselCount > 1) {
                    val current = binding.vpCarousel.currentItem
                    val next = (current + 1) % carouselCount
                    binding.vpCarousel.setCurrentItem(next, true)
                    carouselHandler.postDelayed(this, 4000)
                }
            }
        }
        carouselHandler.postDelayed(carouselRunnable!!, 4000)
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.showBottomNav()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        carouselRunnable?.let { carouselHandler.removeCallbacks(it) }
        _binding = null
    }
}
