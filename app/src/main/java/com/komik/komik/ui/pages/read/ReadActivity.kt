package com.komik.komik.ui.pages.read

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.komik.komik.R
import com.komik.komik.databinding.ActivityReadBinding
import com.komik.komik.databinding.BottomsheetChapterListBinding
import com.komik.komik.databinding.BottomsheetReadSettingsBinding
import com.komik.komik.service.adapter.AdapterChapter
import com.komik.komik.service.adapter.AdapterImage
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.dao.LibraryDao
import com.komik.komik.service.utils.ThemeManager
import com.komik.komik.service.data.dao.chapterDao
import com.komik.komik.service.data.dao.riwayatRelationDao
import com.komik.komik.service.data.model.Chapter
import com.komik.komik.service.data.model.ChapterwithComic
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.komik.komik.service.data.model.Library
import com.komik.komik.service.data.model.Riwayats
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.parser.model.getImages
import com.komik.komik.service.utils.CloudSyncHelper
import com.komik.komik.service.utils.PreferenceManager
import com.komik.komik.service.utils.formatChapterNumber
import com.komik.komik.service.utils.typeSource
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReadBinding
    private lateinit var viewmodel: ReadViewmodel
    private lateinit var adapter: AdapterImage

    private lateinit var db: DatabaseRiwayats
    private lateinit var dao: chapterDao
    private lateinit var riwayats: riwayatRelationDao
    private lateinit var libraryDao: LibraryDao

    private var isHudVisible: Boolean = false
    private var isSliderDragging: Boolean = false
    private var isBookmarked: Boolean = false

    private var isLoadingNextChapter = false
    private var isLoadingPrevChapter = false

    private var currentChapter: Double = 0.0
    private var currentId: Int = 0
    private var pageRead: Int = 1
    private var totalPage: Int = 1
    private var initialPageToScroll: Int = 0

    // Loaded once per comic, re-used for navigation
    private var allChapters: List<Chapter> = emptyList()
    private var currentType: String = ""
    private var currentTitle: String = ""
    private var currentUrl: String = ""
    private var currentLocation: sourceType = sourceType.komikcast
    private var comicId: Int = 0

    // Auto scroll state
    private var isAutoScrolling = false
    private var autoScrollSpeed = 2
    private var autoScrollJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityReadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initDatabase()
        initView()

        viewmodel = ViewModelProvider(this)[ReadViewmodel::class.java]

        currentUrl = intent.getStringExtra(EXTRA_URL).toString()
        currentTitle = intent.getStringExtra(EXTRA_TITLE) ?: ""
        currentType = intent.getStringExtra(EXTRA_TYPE).toString()
        val num = intent.getDoubleExtra("num", 0.0)
        comicId = intent.getIntExtra(EXTRA_ID_COMIC, 0)
        currentChapter = num
        currentId = comicId

        initialPageToScroll = intent.getIntExtra("pageRead", 0)
        currentLocation = typeSource.getSourceType(currentType)

        // Setup Header Text
        binding.judulManhwa.text = currentTitle
        binding.tvChapterSubtitle.text = "Chapter ${formatChapterNum(num)}"
        updateQuickPageIndicator(1, 1)

        initRecyclerView()

        // Load all chapters once for navigation
        loadChaptersForNavigation()

        // Check bookmark status
        checkBookmarkStatus()

        val initialChapterId = intent.getIntExtra(EXTRA_ID, 0)
        // Initial image load (with offline download support)
        viewmodel.setImage(currentLocation, currentUrl, if (initialChapterId != 0) initialChapterId else null, this)

        // Observe images
        viewmodel.image.observe(this) { list ->
            val hasNext = findNextChapter(currentChapter, allChapters) != null
            adapter.updateList(
                newList = list,
                newReferer = currentUrl,
                chapterNum = formatChapterNum(currentChapter),
                hasNext = hasNext
            )
            setupSlider(list.size)
            updateQuickPageIndicator(1, list.size)
            if (initialPageToScroll > 0) {
                val targetPage = initialPageToScroll
                initialPageToScroll = 0 // Hanya berlaku satu kali saat pertama kali dibuka dari Riwayat/Detail
                binding.listImage.post {
                    binding.listImage.scrollToPosition(targetPage)
                }
            } else {
                binding.listImage.post {
                    binding.listImage.scrollToPosition(0)
                }
            }

            val isVip = PreferenceManager.getInstance(this@ReadActivity).isVipOrAbove()
            binding.tvTurboBadge?.visibility = if (isVip) View.VISIBLE else View.GONE
            preloadImagesWithGlide(list, currentUrl, isVip)
        }

        viewmodel.loading.observe(this) { loading ->
            binding.loading.visibility = if (loading) View.VISIBLE else View.GONE
            if (!loading) {
                isLoadingNextChapter = false
                isLoadingPrevChapter = false
            }
        }

        viewmodel.error.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        }

        setupScrollListener()
        setupHudListeners()
    }

    // ---------------------
    // HUD Listeners & Toggling
    // ---------------------
    private fun setupHudListeners() {
        // Tap on empty space outside floating bars toggles HUD
        binding.chapterControls.setOnClickListener {
            toggleHud()
        }

        // Prevent click events on floating bars from closing the HUD
        binding.topBar.setOnClickListener { /* Consume click */ }
        binding.bottomProgress.setOnClickListener { /* Consume click */ }

        binding.back.setOnClickListener {
            finish()
        }

        // Bookmark button
        binding.btnBookmark.setOnClickListener {
            toggleBookmark()
        }

        // Refresh button
        binding.btnRefresh.setOnClickListener {
            val currentChObj = allChapters.firstOrNull { it.numChapter == currentChapter }
            viewmodel.setImage(currentLocation, currentUrl, currentChObj?.id, this@ReadActivity)
            Toast.makeText(this, "Memuat ulang halaman...", Toast.LENGTH_SHORT).show()
        }

        // Report broken chapter button
        binding.btnReportChapter?.setOnClickListener {
            val title = currentTitle.ifEmpty { binding.judulManhwa.text.toString().ifEmpty { "Komik" } }
            val chSubtitle = binding.tvChapterSubtitle.text.toString().ifEmpty { "Chapter $currentChapter" }
            val source = currentLocation.name.ifEmpty { "Ekstensi" }
            val url = currentUrl
            val bs = com.komik.komik.service.component.BottomSheetReport.newInstance(
                sourceName = source,
                comicTitle = title,
                chapterTitle = chSubtitle,
                targetUrl = url,
                defaultType = "image_failed"
            )
            bs.show(supportFragmentManager, com.komik.komik.service.component.BottomSheetReport.TAG)
        }

        // Chapter list bottom sheet
        binding.daftarChapter.setOnClickListener {
            showChapterListSheet()
        }

        // Read settings bottom sheet
        binding.btnSettings.setOnClickListener {
            showReadSettingsSheet()
        }

        // Prev / Next chapter buttons
        binding.btnPrevChapter.setOnClickListener {
            triggerPrevChapter()
        }

        binding.btnNextChapter.setOnClickListener {
            triggerNextChapter()
        }

        // Slider navigation with Tooltip & real-time jump
        binding.slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                isSliderDragging = true
            }

            override fun onStopTrackingTouch(slider: Slider) {
                isSliderDragging = false
                val pos = slider.value.toInt().coerceIn(0, adapter.itemCount - 1)
                binding.listImage.scrollToPosition(pos)
            }
        })

        binding.slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val pos = value.toInt().coerceIn(0, adapter.itemCount - 1)
                binding.listImage.scrollToPosition(pos)
            }
        }
    }

    private fun toggleHud() {
        if (isHudVisible) {
            hideHud()
        } else {
            showHud()
        }
    }

    private fun showHud() {
        isHudVisible = true
        // Fade out floating page pill so it doesn't overlap bottom HUD
        binding.pillPageInfo.animate()
            .alpha(0f)
            .setDuration(180)
            .withEndAction { binding.pillPageInfo.visibility = View.GONE }
            .start()

        binding.chapterControls.visibility = View.VISIBLE
        binding.chapterControls.alpha = 0f
        binding.chapterControls.animate()
            .alpha(1f)
            .setDuration(220)
            .setListener(null)
            .start()
    }

    private fun hideHud() {
        isHudVisible = false
        // Fade in floating page pill
        binding.pillPageInfo.visibility = View.VISIBLE
        binding.pillPageInfo.alpha = 0f
        binding.pillPageInfo.animate()
            .alpha(1f)
            .setDuration(200)
            .start()

        binding.chapterControls.animate()
            .alpha(0f)
            .setDuration(200)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.chapterControls.visibility = View.GONE
                }
            })
            .start()
    }

    // ---------------------
    // Scroll Listener
    // ---------------------
    private fun setupScrollListener() {
        val pref = getSharedPreferences("app_settings", MODE_PRIVATE)

        binding.listImage.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                // Auto-Hide HUD when scrolling down
                if (dy > 8 && isHudVisible) {
                    hideHud()
                }

                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val totalItem = adapter.itemCount
                val imageCount = if (totalItem > 0) totalItem - 1 else 0 // subtract end footer
                val firstVisible = layoutManager.findFirstVisibleItemPosition()

                if (imageCount <= 0 || firstVisible < 0) return

                val currentPage = (firstVisible + 1).coerceAtMost(imageCount)
                pageRead = currentPage
                totalPage = imageCount

                // Update floating pill and bottom bar text
                updateQuickPageIndicator(currentPage, imageCount)

                // Update 2dp reading progress bar at top
                val progress = ((currentPage.toFloat() / imageCount.toFloat()) * 100f).toInt()
                binding.readingProgressBar.progress = progress

                // Sync slider value
                if (!isSliderDragging) {
                    val sliderVal = firstVisible.toFloat().coerceIn(binding.slider.valueFrom, binding.slider.valueTo)
                    if (binding.slider.value != sliderVal) {
                        runCatching { binding.slider.value = sliderVal }
                    }
                }

                // Smart Pre-load: when reaching >= 80% of current chapter images
                val percentage = currentPage.toFloat() / imageCount.toFloat()
                if (percentage >= 0.8f && !viewmodel.isPreloadingNext) {
                    val nextCh = findNextChapter(currentChapter, allChapters)
                    if (nextCh != null && viewmodel.preloadedNextChapterId != nextCh.id) {
                        viewmodel.preloadNextChapter(
                            this@ReadActivity,
                            currentLocation,
                            nextCh.id,
                            nextCh.link
                        )
                    }
                }

                // Reset prev lock early so user can navigate backward
                if (firstVisible >= 3) {
                    isLoadingPrevChapter = false
                }

                // Auto chapter switch when reaching end of list
                val autoSwitch = pref.getBoolean("pindah", true)
                if (!autoSwitch) return

                if (dy > 0 && !isLoadingNextChapter && !recyclerView.canScrollVertically(1)) {
                    isLoadingNextChapter = true
                    triggerNextChapter()
                }

                if (dy < 0 && !isLoadingPrevChapter && !recyclerView.canScrollVertically(-1)) {
                    isLoadingPrevChapter = true
                    triggerPrevChapter()
                }
            }
        })
    }

    private fun updateQuickPageIndicator(current: Int, total: Int) {
        val chNum = formatChapterNum(currentChapter)
        binding.tvQuickChapterInfo.text = "Ch. $chNum • $current/$total"
        binding.PageNumber.text = "$current/$total"
    }

    // ---------------------
    // Chapter Navigation Math & Pre-load
    // ---------------------
    private fun loadChaptersForNavigation() {
        CoroutineScope(Dispatchers.IO).launch {
            val chapters = dao.getChapterForRead(currentTitle, currentType)
            withContext(Dispatchers.Main) {
                allChapters = chapters
                val hasNext = findNextChapter(currentChapter, allChapters) != null
                adapter.updateChapterInfo(formatChapterNum(currentChapter), hasNext)
                Log.d(TAG, "loadChaptersForNavigation: ${chapters.size} chapters loaded")
            }
        }
    }

    private fun triggerNextChapter() {
        val next = findNextChapter(currentChapter, allChapters)
        if (next != null) {
            switchToChapter(next)
        } else {
            isLoadingNextChapter = false
            Snackbar.make(binding.root, "Sudah chapter terbaru", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun triggerPrevChapter() {
        val prev = findPrevChapter(currentChapter, allChapters)
        if (prev != null) {
            switchToChapter(prev)
        } else {
            isLoadingPrevChapter = false
            Snackbar.make(binding.root, "Sudah chapter paling awal", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun switchToChapter(chapter: Chapter) {
        CoroutineScope(Dispatchers.IO).launch {
            insertRiwayat(chapter.id, comicId)
            dao.updateIsRead(chapter.id, true)
            withContext(Dispatchers.Main) {
                initialPageToScroll = 0
                pageRead = 1
                adapter.clear()
                binding.listImage.scrollToPosition(0)
                currentChapter = chapter.numChapter
                currentUrl = chapter.link
                binding.judulManhwa.text = currentTitle
                binding.tvChapterSubtitle.text = "Chapter ${formatChapterNum(chapter.numChapter)}"
                updateQuickPageIndicator(1, 1)

                val hasNext = findNextChapter(chapter.numChapter, allChapters) != null
                adapter.updateChapterInfo(formatChapterNum(chapter.numChapter), hasNext)

                if (isHudVisible) hideHud()

                // Passes chapter.id & context to viewmodel.setImage so preloaded cache or offline downloads are utilized instantly!
                viewmodel.setImage(currentLocation, chapter.link, chapter.id, this@ReadActivity)
            }
        }
    }

    private fun findNextChapter(current: Double, all: List<Chapter>): Chapter? {
        val idx = all.indexOfFirst { it.numChapter == current }
        return if (idx >= 0 && idx + 1 < all.size) all[idx + 1] else null
    }

    private fun findPrevChapter(current: Double, all: List<Chapter>): Chapter? {
        val idx = all.indexOfFirst { it.numChapter == current }
        return if (idx > 0) all[idx - 1] else null
    }

    // ---------------------
    // Bookmark Pustaka
    // ---------------------
    private fun checkBookmarkStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            val existing = libraryDao.getLibraryByComicId(comicId)
            withContext(Dispatchers.Main) {
                isBookmarked = existing != null
                updateBookmarkIcon()
            }
        }
    }

    private fun toggleBookmark() {
        CoroutineScope(Dispatchers.IO).launch {
            if (isBookmarked) {
                libraryDao.deleteLibrary(comicId)
                isBookmarked = false
                withContext(Dispatchers.Main) {
                    updateBookmarkIcon()
                    Snackbar.make(binding.root, "Dihapus dari Pustaka", Snackbar.LENGTH_SHORT).show()
                }
            } else {
                libraryDao.insertLibrary(
                    Library(
                        comicId = comicId,
                        isFavorite = true,
                        createdAt = System.currentTimeMillis()
                    )
                )
                isBookmarked = true
                withContext(Dispatchers.Main) {
                    updateBookmarkIcon()
                    Snackbar.make(binding.root, "Disimpan ke Pustaka", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateBookmarkIcon() {
        if (isBookmarked) {
            binding.btnBookmark.setImageResource(R.drawable.bookmark_simple_fill)
            binding.btnBookmark.setColorFilter(getColor(R.color.brand))
        } else {
            binding.btnBookmark.setImageResource(R.drawable.bookmark_simple)
            binding.btnBookmark.setColorFilter(getColor(R.color.white))
        }
    }

    // ---------------------
    // Auto-Scroll Feature
    // ---------------------
    private fun startAutoScroll(speed: Int) {
        autoScrollSpeed = speed
        isAutoScrolling = true
        autoScrollJob?.cancel()
        autoScrollJob = CoroutineScope(Dispatchers.Main).launch {
            while (isAutoScrolling) {
                binding.listImage.scrollBy(0, autoScrollSpeed * 2)
                delay(16)
            }
        }
    }

    private fun stopAutoScroll() {
        isAutoScrolling = false
        autoScrollJob?.cancel()
    }

    private fun preloadImagesWithGlide(list: List<getImages>, refererUrl: String?, isVip: Boolean) {
        if (list.isEmpty()) return
        val type = currentLocation
        // VIP: Pre-load paralel seluruh halaman dalam chapter untuk zero-loading saat scroll cepat
        // Member: Pre-load standar 2 halaman pertama ke depan
        val pagesToPreload = if (isVip) list else list.take(2)

        CoroutineScope(Dispatchers.IO).launch {
            for (item in pagesToPreload) {
                val imageStr = item.image.trim()
                if (imageStr.isEmpty()) continue
                val isLocal = imageStr.startsWith("/") || imageStr.startsWith("file://")

                try {
                    if (isLocal) {
                        val localPath = if (imageStr.startsWith("file://")) imageStr.substring(7) else imageStr
                        val file = File(localPath)
                        if (file.exists() && file.length() > 0L) {
                            Glide.with(applicationContext).load(file).preload()
                        }
                    } else {
                        val glideModel: Any = when {
                            type == sourceType.doujindesu || imageStr.contains("desu.pics") || imageStr.contains("doujindesu") -> {
                                val effectiveReferer = if (!refererUrl.isNullOrBlank() && refererUrl.startsWith("http")) refererUrl else "https://doujin.desu.xxx/"
                                val headers = LazyHeaders.Builder()
                                    .addHeader("Referer", effectiveReferer)
                                    .addHeader("Origin", "https://doujin.desu.xxx")
                                    .addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6_1; like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1")
                                    .addHeader("X-Requested-With", "XMLHttpRequest")
                                    .build()
                                GlideUrl(imageStr, headers)
                            }
                            imageStr.contains("shngm.id") || type == sourceType.shinigami -> {
                                val headers = LazyHeaders.Builder()
                                    .addHeader("Referer", "https://app.shinigami.asia/")
                                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                                    .build()
                                GlideUrl(imageStr, headers)
                            }
                            !refererUrl.isNullOrBlank() && refererUrl.startsWith("http") -> {
                                val headers = LazyHeaders.Builder()
                                    .addHeader("Referer", refererUrl)
                                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                                    .build()
                                GlideUrl(imageStr, headers)
                            }
                            else -> imageStr
                        }
                        Glide.with(applicationContext).load(glideModel).preload()
                    }
                } catch (_: Exception) {
                    // Ignore preload error in background
                }
            }
        }
    }

    // ---------------------
    // BottomSheet: Chapter List
    // ---------------------
    private fun showChapterListSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomsheetChapterListBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        val dummyComic = com.komik.komik.service.data.model.Comic(
            id = comicId,
            title = currentTitle,
            url = "",
            imageUrl = "",
            description = "",
            author = "",
            location = currentType,
            rating = ""
        )
        val chapterWithComicList: List<ChapterwithComic> = allChapters.map { ch ->
            ChapterwithComic(ch, dummyComic)
        }

        val chapterAdapter = AdapterChapter(chapterWithComicList) { chapterItem ->
            dialog.dismiss()
            switchToChapter(chapterItem.chapter)
        }
        chapterAdapter.setLastReadChapterId(
            allChapters.firstOrNull { it.numChapter == currentChapter }?.id
        )

        sheetBinding.rvChapterSheet.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        sheetBinding.rvChapterSheet.adapter = chapterAdapter
        sheetBinding.btnCloseSheet.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    // ---------------------
    // BottomSheet: Read Settings
    // ---------------------
    private fun showReadSettingsSheet() {
        val dialog = BottomSheetDialog(this)
        val settingsBinding = BottomsheetReadSettingsBinding.inflate(layoutInflater)
        dialog.setContentView(settingsBinding.root)

        val pref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val currentMode = pref.getString("reading_mode", "webtoon")
        val isFullscreen = pref.getBoolean("fullscreen", true)
        val isAutoChapter = pref.getBoolean("pindah", true)
        val readerBrightness = pref.getFloat("reader_brightness", -1f)
        val savedAutoScrollSpeed = pref.getInt("auto_scroll_speed", 2)

        // Reading Mode buttons
        fun updateModeButtons(mode: String) {
            if (mode == "horizontal") {
                settingsBinding.btnModeHorizontal.setBackgroundResource(R.drawable.bg_read_btn_accent)
                settingsBinding.btnModeWebtoon.setBackgroundResource(R.drawable.bg_read_btn)
            } else {
                settingsBinding.btnModeWebtoon.setBackgroundResource(R.drawable.bg_read_btn_accent)
                settingsBinding.btnModeHorizontal.setBackgroundResource(R.drawable.bg_read_btn)
            }
        }
        updateModeButtons(currentMode ?: "webtoon")

        settingsBinding.btnModeWebtoon.setOnClickListener {
            pref.edit().putString("reading_mode", "webtoon").apply()
            updateModeButtons("webtoon")
            binding.listImage.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        }

        settingsBinding.btnModeHorizontal.setOnClickListener {
            pref.edit().putString("reading_mode", "horizontal").apply()
            updateModeButtons("horizontal")
            binding.listImage.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        }

        // Screen Brightness
        val isSystemBrightness = readerBrightness < 0
        settingsBinding.switchSystemBrightness.isChecked = isSystemBrightness
        settingsBinding.sliderBrightness.isEnabled = !isSystemBrightness
        if (!isSystemBrightness) {
            settingsBinding.sliderBrightness.value = readerBrightness.coerceIn(0.05f, 1.0f)
        }

        settingsBinding.switchSystemBrightness.setOnCheckedChangeListener { _, isChecked ->
            settingsBinding.sliderBrightness.isEnabled = !isChecked
            if (isChecked) {
                pref.edit().putFloat("reader_brightness", -1f).apply()
                applyBrightness(-1f)
            } else {
                val brightnessVal = settingsBinding.sliderBrightness.value
                pref.edit().putFloat("reader_brightness", brightnessVal).apply()
                applyBrightness(brightnessVal)
            }
        }

        settingsBinding.sliderBrightness.addOnChangeListener { _, value, fromUser ->
            if (fromUser && !settingsBinding.switchSystemBrightness.isChecked) {
                pref.edit().putFloat("reader_brightness", value).apply()
                applyBrightness(value)
            }
        }

        // Auto-Scroll
        settingsBinding.switchAutoScroll.isChecked = isAutoScrolling
        settingsBinding.layoutAutoScrollSpeed.visibility = if (isAutoScrolling) View.VISIBLE else View.GONE
        settingsBinding.sliderAutoScrollSpeed.value = savedAutoScrollSpeed.toFloat()
        settingsBinding.tvAutoScrollSpeed.text = "Kecepatan Gulir: ${savedAutoScrollSpeed}x"

        settingsBinding.switchAutoScroll.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                val isVip = PreferenceManager.getInstance(this@ReadActivity).isVipOrAbove()
                if (!isVip) {
                    buttonView.isChecked = false
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(this@ReadActivity)
                        .setTitle("Fitur VIP: Auto-Scroll")
                        .setMessage("Fitur membaca hands-free (Auto-Scroll) otomatis hanya tersedia untuk pengguna VIP / Moderator / Admin.\n\nUpgrade akun Anda untuk menikmati membaca komik tanpa perlu menyentuh layar!")
                        .setPositiveButton("Mengerti", null)
                        .show()
                    return@setOnCheckedChangeListener
                }
                settingsBinding.layoutAutoScrollSpeed.visibility = View.VISIBLE
                startAutoScroll(settingsBinding.sliderAutoScrollSpeed.value.toInt())
            } else {
                settingsBinding.layoutAutoScrollSpeed.visibility = View.GONE
                stopAutoScroll()
            }
        }

        settingsBinding.sliderAutoScrollSpeed.addOnChangeListener { _, value, fromUser ->
            val speed = value.toInt()
            settingsBinding.tvAutoScrollSpeed.text = "Kecepatan Gulir: ${speed}x"
            if (fromUser) {
                pref.edit().putInt("auto_scroll_speed", speed).apply()
                if (isAutoScrolling) {
                    startAutoScroll(speed)
                }
            }
        }

        // Fullscreen & Auto Chapter
        settingsBinding.switchFullscreen.isChecked = isFullscreen
        settingsBinding.switchAutoChapter.isChecked = isAutoChapter

        settingsBinding.switchFullscreen.setOnCheckedChangeListener { _, isChecked ->
            pref.edit().putBoolean("fullscreen", isChecked).apply()
            if (isChecked) hideUi() else showUi()
        }

        settingsBinding.switchAutoChapter.setOnCheckedChangeListener { _, isChecked ->
            pref.edit().putBoolean("pindah", isChecked).apply()
        }

        dialog.show()
    }

    private fun applyBrightness(brightness: Float) {
        val lp = window.attributes
        lp.screenBrightness = if (brightness < 0) WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE else brightness
        window.attributes = lp
    }

    // ---------------------
    // Slider Setup
    // ---------------------
    private fun setupSlider(size: Int) {
        if (size < 2) {
            binding.slider.visibility = View.GONE
            return
        }
        binding.slider.visibility = View.VISIBLE
        try {
            binding.slider.valueFrom = 0f
            binding.slider.valueTo = (size - 1).toFloat()
            binding.slider.value = 0f
            binding.slider.stepSize = 1f
            binding.slider.setLabelFormatter { value ->
                "Hal. ${value.toInt() + 1} / $size"
            }
        } catch (_: Exception) {}
    }

    // ---------------------
    // DB Operations
    // ---------------------
    private suspend fun insertRiwayat(chapterId: Int, comicId: Int) {
        val existing = riwayats.getRiwayatByChapterId(comicId)
        if (existing == null) {
            riwayats.insertRiwayatRelasi(
                Riwayats(
                    chapterId = chapterId,
                    comicId = comicId,
                    readAt = System.currentTimeMillis(),
                    isRead = true
                )
            )
        } else {
            riwayats.updateRiwayatRelasi(
                existing.copy(
                    chapterId = chapterId,
                    comicId = comicId,
                    isRead = true,
                    readAt = System.currentTimeMillis()
                )
            )
        }
    }

    private fun saveProgress() {
        CoroutineScope(Dispatchers.IO).launch {
            riwayats.updateProgress(currentId, pageRead, totalPage)
            Log.d(TAG, "saveProgress: comicId=$currentId page=$pageRead/$totalPage")
        }
    }

    // ---------------------
    // Lifecycle
    // ---------------------
    override fun onPause() {
        super.onPause()
        saveProgress()
        stopAutoScroll()
    }

    override fun onResume() {
        super.onResume()
        val pref = getSharedPreferences("app_settings", MODE_PRIVATE)
        if (pref.getBoolean("fullscreen", true)) hideUi() else showUi()
        if (pref.getBoolean("keep_screen_on", true)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        val brightness = pref.getFloat("reader_brightness", -1f)
        applyBrightness(brightness)
    }

    // ---------------------
    // UI Setup
    // ---------------------
    private fun initDatabase() {
        db = DatabaseRiwayats.getDatabase(this)
        dao = db.chapterdao()
        riwayats = db.riwayatdao()
        libraryDao = db.libraryDao()
    }

    private fun initRecyclerView() {
        val pref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val mode = pref.getString("reading_mode", "webtoon")
        val layoutManager = if (mode == "horizontal") {
            LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        } else {
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        }
        binding.listImage.layoutManager = layoutManager

        adapter = AdapterImage(
            list = mutableListOf(),
            type = currentLocation,
            refererUrl = currentUrl,
            chapterNumberText = formatChapterNum(currentChapter),
            hasNextChapter = true,
            onSingleTap = { toggleHud() },
            onNextChapterClick = { triggerNextChapter() },
            onBackToDetailClick = { finish() }
        )
        binding.listImage.adapter = adapter

        // Single tap detector on RecyclerView so tapping anywhere toggles HUD instantly
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                toggleHud()
                return true
            }
        })

        binding.listImage.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(e)
                return false
            }
        })
    }

    private fun initView() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sysBars.left, 0, sysBars.right, 0)
            insets
        }
    }

    @Suppress("DEPRECATION")
    fun hideUi() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }

    @Suppress("DEPRECATION")
    fun showUi() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    private fun formatChapterNum(num: Double): String {
        return num.formatChapterNumber()
    }

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_ID = "extra_id"
        const val EXTRA_ID_COMIC = "extra_id_comic"
        const val TAG = "ReadActivity"
    }
}
