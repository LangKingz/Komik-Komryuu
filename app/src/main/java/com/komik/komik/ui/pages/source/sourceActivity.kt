package com.komik.komik.ui.pages.source

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.komik.komik.R
import com.komik.komik.databinding.ActivitySourceBinding
import com.komik.komik.databinding.BottomsheetFilterSourceBinding
import com.komik.komik.service.adapter.AdapterManhwa
import com.komik.komik.service.adapter.AdapterRiwayatSource
import com.komik.komik.service.adapter.AdapterSource
import com.komik.komik.service.component.BottomSheetCookie
import com.komik.komik.service.component.BottomsheetInfo
import com.komik.komik.service.data.model.RiwayatDetail
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.parser.model.getListUpdate
import com.komik.komik.service.utils.isNetworkAvaible
import com.komik.komik.service.utils.typeSource

class sourceActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySourceBinding
    private lateinit var viewmodel: sourceViewmodel
    private lateinit var adapterManhwa: AdapterManhwa
    private lateinit var adapterRiwayatSource: AdapterRiwayatSource

    private lateinit var locations: String
    private var urlSource: String = ""
    private var size: String = "0"
    private var fullComicList = mutableListOf<getListUpdate>()
    private var currentCategory = "Semua"
    private var isCurrentSourcePinned = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySourceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        compatView()

        window.navigationBarColor = ContextCompat.getColor(this, R.color.bottom_navbar)
        viewmodel = ViewModelProvider(this)[sourceViewmodel::class.java]

        val url = intent.getStringExtra(URL).orEmpty()
        val location = intent.getStringExtra(location).orEmpty()
        urlSource = url
        locations = location

        setupHeader()
        setupRecyclerViews()

        val source = typeSource.getSourceType(locations)
        initPinState()
        initServerStatusState()

        viewmodel.getSource(source, urlSource, 30)

        setupQuickFilterChips(source)
        setupRealtimeSearch(source)
        setupSwipeRefresh(source)
        setupFloatingBottomBar(source)
        setupErrorHandling(source)
        observeViewModel()

        if (intent.getBooleanExtra(EXTRA_AUTO_FOCUS_SEARCH, false)) {
            binding.etSearchSource.post {
                binding.etSearchSource.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(binding.etSearchSource, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun setupHeader() {
        binding.tvSourceName.text = locations.ifEmpty { "Sumber Ekstensi" }
        binding.tvSourceDomain.text = urlSource.ifEmpty { "Online" }

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Extension Logo / Avatar
        binding.tvSourceAvatar.text = locations.firstOrNull()?.toString()?.uppercase() ?: "S"
        val logoUrl = when (locations.lowercase()) {
            "ikiru" -> "https://ikiru.id/favicon-32x32.png"
            "comicscan" -> "https://lc3.cosmicscans.asia/wp-content/uploads/2024/03/logo_cosmic.png"
            "shinigami" -> "https://cdn.jsdelivr.net/gh/shinigamiscans/static@latest/emotes/3.webp"
            "komikcast" -> "https://komikcast.li/wp-content/uploads/2021/02/cropped-logo-kc-32x32.png"
            "kiryuu" -> "https://kiryuu02.com/wp-content/uploads/2021/10/cropped-logo-icon-kiryuu-1-456248-udlqjluy-194445-3fNc9Wlc-32x32.png"
            else -> ""
        }

        if (logoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(logoUrl)
                .placeholder(R.drawable.corner_gambar)
                .error(R.drawable.ppanime)
                .into(binding.ivSourceLogo)
            binding.ivSourceLogo.visibility = View.VISIBLE
            binding.tvSourceAvatar.visibility = View.GONE
        } else {
            binding.ivSourceLogo.visibility = View.GONE
            binding.tvSourceAvatar.visibility = View.VISIBLE
        }

        // Open in Browser Quick Action
        binding.btnOpenBrowser.setOnClickListener {
            if (urlSource.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlSource))
                startActivity(intent)
            }
        }

        // Cookie / Bypass Quick Action
        binding.btnCookieBypass.setOnClickListener {
            val bottomSheet = BottomSheetCookie()
            bottomSheet.show(supportFragmentManager, "source_cookie_bs")
        }

        // Source Info Button
        binding.btnSourceInfo.setOnClickListener {
            val bottomSheet = BottomsheetInfo.newInstance(
                url = urlSource,
                size = size,
                name = locations,
                logo = logoUrl
            )
            bottomSheet.show(supportFragmentManager, "source_info_bs")
        }

        // Hide History Section button
        binding.btnHideHistorySection.setOnClickListener {
            binding.layoutSourceHistorySection.visibility = View.GONE
        }
    }

    private fun setupRecyclerViews() {
        // Main Comic List
        adapterManhwa = AdapterManhwa(mutableListOf())
        binding.listSource.layoutManager = GridLayoutManager(this, 3)
        binding.listSource.adapter = adapterManhwa

        // Source Specific History List (Horizontal)
        adapterRiwayatSource = AdapterRiwayatSource(mutableListOf())
        binding.rvSourceHistory.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvSourceHistory.adapter = adapterRiwayatSource
    }

    private fun setupQuickFilterChips(source: sourceType) {
        binding.chipTerbaru.setOnClickListener {
            binding.layoutSourceHistorySection.visibility = View.GONE
            adapterManhwa.updateData(fullComicList)
            binding.emptySearchState.visibility = if (fullComicList.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.chipPopuler.setOnClickListener {
            binding.layoutSourceHistorySection.visibility = View.GONE
            // Sort by tag or shuffle to show variety
            val popularList = fullComicList.sortedByDescending { it.chapter ?: "" }
            adapterManhwa.updateData(popularList)
            binding.emptySearchState.visibility = if (popularList.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.chipRiwayatSource.setOnClickListener {
            binding.layoutSourceHistorySection.visibility = View.VISIBLE
        }
    }

    private fun setupRealtimeSearch(source: sourceType) {
        binding.etSearchSource.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty().trim()
                binding.btnCloseSourceSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE

                if (query.isNotEmpty()) {
                    val localMatches = fullComicList.filter {
                        it.title?.contains(query, ignoreCase = true) == true
                    }
                    if (localMatches.isNotEmpty()) {
                        adapterManhwa.updateData(localMatches)
                        binding.emptySearchState.visibility = View.GONE
                    } else if (query.length >= 3) {
                        viewmodel.search(source, query, urlSource)
                    }
                } else {
                    adapterManhwa.updateData(fullComicList)
                    binding.emptySearchState.visibility = if (fullComicList.isEmpty()) View.VISIBLE else View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnCloseSourceSearch.setOnClickListener {
            binding.etSearchSource.setText("")
            binding.btnCloseSourceSearch.visibility = View.GONE
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(binding.etSearchSource.windowToken, 0)
            adapterManhwa.updateData(fullComicList)
            binding.emptySearchState.visibility = if (fullComicList.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupSwipeRefresh(source: sourceType) {
        binding.swipeRefreshSource.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.primary_button)
        )
        binding.swipeRefreshSource.setOnRefreshListener {
            binding.error.visibility = View.GONE
            updateServerStatus(isOnline = true, isChecking = true)
            viewmodel.getSource(source, urlSource, 30)
        }
    }

    private fun setupErrorHandling(source: sourceType) {
        binding.again.setOnClickListener {
            binding.error.visibility = View.GONE
            updateServerStatus(isOnline = true, isChecking = true)
            viewmodel.getSource(source, urlSource, 30)
        }

        binding.btnErrorCookie.setOnClickListener {
            val bottomSheet = BottomSheetCookie()
            bottomSheet.show(supportFragmentManager, "error_cookie_bs")
        }
    }

    private fun initPinState() {
        val pref = getSharedPreferences(AdapterSource.PREF_PINNED, Context.MODE_PRIVATE)
        val pinnedKeys = pref.getStringSet(AdapterSource.KEY_PINNED, emptySet()) ?: emptySet()
        isCurrentSourcePinned = pinnedKeys.contains(locations) || pinnedKeys.contains(urlSource)
        updatePinUi(isCurrentSourcePinned)
    }

    private fun updatePinUi(pinned: Boolean) {
        if (pinned) {
            binding.icPinSource.setImageResource(R.drawable.push_pin_fill)
            binding.icPinSource.setColorFilter(ContextCompat.getColor(this, R.color.color_star))
            binding.tvPinSourceLabel.text = "Tersimpan"
        } else {
            binding.icPinSource.setImageResource(R.drawable.push_pin)
            binding.icPinSource.setColorFilter(ContextCompat.getColor(this, R.color.gray_400))
            binding.tvPinSourceLabel.text = "Pin"
        }
    }

    private fun togglePinCurrentSource() {
        val pref = getSharedPreferences(AdapterSource.PREF_PINNED, Context.MODE_PRIVATE)
        val currentSet = HashSet(pref.getStringSet(AdapterSource.KEY_PINNED, emptySet()) ?: emptySet())
        val key = locations.ifEmpty { urlSource }

        isCurrentSourcePinned = if (currentSet.contains(key)) {
            currentSet.remove(key)
            false
        } else {
            currentSet.add(key)
            true
        }

        pref.edit().putStringSet(AdapterSource.KEY_PINNED, currentSet).apply()

        // Animasi scaling pada ikon pin
        val scaleAnim = ScaleAnimation(
            0.7f, 1.2f, 0.7f, 1.2f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 200
            repeatMode = Animation.REVERSE
            repeatCount = 1
        }
        binding.icPinSource.startAnimation(scaleAnim)

        updatePinUi(isCurrentSourcePinned)

        val message = if (isCurrentSourcePinned) {
            "Sumber '$locations' berhasil disematkan ke atas!"
        } else {
            "Pin sumber '$locations' dilepas."
        }

        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setTextColor(ContextCompat.getColor(this, R.color.white))
            .setBackgroundTint(ContextCompat.getColor(this, R.color.bottom_navbar))
            .show()
    }

    private fun initServerStatusState() {
        val hasInternet = isNetworkAvaible(this)
        updateServerStatus(isOnline = hasInternet && urlSource.isNotEmpty())
    }

    private fun updateServerStatus(isOnline: Boolean, isChecking: Boolean = false) {
        val ping = viewmodel.pingMs.value
        val pingSuffix = if (ping != null && isOnline) " • ${ping}ms" else ""

        if (isChecking) {
            binding.badgeServerStatusPill.text = "● Memeriksa"
            binding.badgeServerStatusPill.setBackgroundResource(R.drawable.badge_status_online)
            binding.badgeServerStatusPill.setTextColor(ContextCompat.getColor(this, R.color.golden_sand))

            binding.badgeServerStatusTop.text = "● Memeriksa"
            binding.badgeServerStatusTop.setBackgroundResource(R.drawable.badge_status_online)
            binding.badgeServerStatusTop.setTextColor(ContextCompat.getColor(this, R.color.golden_sand))
        } else if (isOnline) {
            val text = "● Aktif$pingSuffix"
            binding.badgeServerStatusPill.text = text
            binding.badgeServerStatusPill.setBackgroundResource(R.drawable.badge_status_online)
            binding.badgeServerStatusPill.setTextColor(ContextCompat.getColor(this, R.color.text_emerald_400))

            binding.badgeServerStatusTop.text = "● Online$pingSuffix"
            binding.badgeServerStatusTop.setBackgroundResource(R.drawable.badge_status_online)
            binding.badgeServerStatusTop.setTextColor(ContextCompat.getColor(this, R.color.text_emerald_400))
        } else {
            binding.badgeServerStatusPill.text = "● Offline"
            binding.badgeServerStatusPill.setBackgroundResource(R.drawable.badge_status_offline)
            binding.badgeServerStatusPill.setTextColor(ContextCompat.getColor(this, R.color.red))

            binding.badgeServerStatusTop.text = "● Offline"
            binding.badgeServerStatusTop.setBackgroundResource(R.drawable.badge_status_offline)
            binding.badgeServerStatusTop.setTextColor(ContextCompat.getColor(this, R.color.red))
        }
    }

    private fun setupFloatingBottomBar(source: sourceType) {
        // 1. Filter Kategori
        binding.btnFilterKategori.setOnClickListener {
            showCategoryFilterBottomSheet()
        }

        // 2. Pencarian Cepat -> Focus inline search bar
        binding.btnCariSource.setOnClickListener {
            binding.etSearchSource.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(binding.etSearchSource, InputMethodManager.SHOW_IMPLICIT)
        }

        // 3. Pin Source ke Atas
        binding.btnPinSource.setOnClickListener {
            togglePinCurrentSource()
        }

        // 4. Badge Status Server Aktif/Offline Click
        binding.btnServerStatus.setOnClickListener {
            val isOnline = binding.badgeServerStatusPill.text.toString().contains("Aktif")
            val statusMessage = if (isOnline) {
                "Server '$locations' Aktif: Terhubung ke $urlSource"
            } else {
                "Server '$locations' Offline: Gagal terhubung ke $urlSource"
            }

            Snackbar.make(binding.root, statusMessage, Snackbar.LENGTH_LONG)
                .setTextColor(ContextCompat.getColor(this, R.color.white))
                .setBackgroundTint(ContextCompat.getColor(this, R.color.bottom_navbar))
                .setAction("Refresh") {
                    updateServerStatus(isOnline = true, isChecking = true)
                    viewmodel.getSource(source, urlSource, 30)
                }
                .show()
        }
    }

    private fun showCategoryFilterBottomSheet() {
        val bottomSheet = BottomSheetDialog(this)
        val sheetBinding = BottomsheetFilterSourceBinding.inflate(layoutInflater)
        bottomSheet.setContentView(sheetBinding.root)

        when (currentCategory) {
            "Komik" -> sheetBinding.rbKategoriKomik.isChecked = true
            "Anime" -> sheetBinding.rbKategoriAnime.isChecked = true
            "Cinema" -> sheetBinding.rbKategoriCinema.isChecked = true
            else -> sheetBinding.rbKategoriSemua.isChecked = true
        }

        sheetBinding.btnTerapkanKategori.setOnClickListener {
            currentCategory = when {
                sheetBinding.rbKategoriKomik.isChecked -> "Komik"
                sheetBinding.rbKategoriAnime.isChecked -> "Anime"
                sheetBinding.rbKategoriCinema.isChecked -> "Cinema"
                else -> "Semua"
            }

            binding.tvFilterKategoriLabel.text = currentCategory
            filterComicsByCategory(currentCategory)
            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }

    private fun filterComicsByCategory(category: String) {
        if (category == "Semua") {
            adapterManhwa.updateData(fullComicList)
            binding.emptySearchState.visibility = if (fullComicList.isEmpty()) View.VISIBLE else View.GONE
        } else {
            val filtered = fullComicList.filter {
                it.title?.contains(category, ignoreCase = true) == true ||
                it.location?.contains(category, ignoreCase = true) == true
            }
            adapterManhwa.updateData(filtered)
            binding.emptySearchState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun observeViewModel() {
        // Source Content
        viewmodel.source.observe(this) { source ->
            size = source.size.toString()
            fullComicList = ArrayList(source)
            adapterManhwa.updateData(source)
            binding.swipeRefreshSource.isRefreshing = false
            binding.emptySearchState.visibility = if (source.isEmpty()) View.VISIBLE else View.GONE
            updateServerStatus(isOnline = true)
        }

        // Loading
        viewmodel.loading.observe(this) { loading ->
            binding.loading.visibility = if (loading) View.VISIBLE else View.GONE
            if (!loading) binding.swipeRefreshSource.isRefreshing = false
        }

        // Ping Latency
        viewmodel.pingMs.observe(this) {
            updateServerStatus(isOnline = true)
        }

        // Search
        viewmodel.search.observe(this) { searchResult ->
            adapterManhwa.updateData(searchResult)
            binding.emptySearchState.visibility = if (searchResult.isEmpty()) View.VISIBLE else View.GONE
        }

        // Error
        viewmodel.error.observe(this) { error ->
            if (error != null) {
                binding.swipeRefreshSource.isRefreshing = false
                updateServerStatus(isOnline = false)
                if (fullComicList.isEmpty()) {
                    binding.error.visibility = View.VISIBLE
                    binding.tvErrorMessage.text = error
                }
            }
        }

        // Specific History for this source
        viewmodel.allHistory.observe(this) { historyList ->
            val sourceHistory = historyList.filter {
                it.comic.location.equals(locations, ignoreCase = true) ||
                it.chapter.location?.equals(locations, ignoreCase = true) == true
            }
            if (sourceHistory.isNotEmpty()) {
                adapterRiwayatSource.updateData(sourceHistory)
                binding.layoutSourceHistorySection.visibility = View.VISIBLE
            } else {
                binding.layoutSourceHistorySection.visibility = View.GONE
            }
        }
    }

    private fun compatView() {
        WindowCompat.getInsetsController(window, window.decorView).let { controller ->
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }
    }

    companion object {
        const val TAG = "sourceActivity"
        const val URL = "url"
        const val location = "location"
        const val EXTRA_AUTO_FOCUS_SEARCH = "AUTO_FOCUS_SEARCH"
    }
}
