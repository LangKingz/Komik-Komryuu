package com.komik.komik.ui.pages.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.komik.komik.R
import com.komik.komik.databinding.ActivityDetailBinding
import com.komik.komik.service.adapter.AdapterChapter
import com.komik.komik.service.adapter.AdapterGenre
import com.komik.komik.service.client
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.utils.ThemeManager
import com.komik.komik.service.data.dao.ComicDao
import com.komik.komik.service.data.dao.LibraryDao
import com.komik.komik.service.data.dao.chapterDao
import com.komik.komik.service.data.dao.genreDao
import com.komik.komik.service.data.dao.riwayatRelationDao
import com.komik.komik.service.data.model.ChapterwithComic
import com.komik.komik.service.data.model.Comic
import com.komik.komik.service.data.model.Library
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.repository.Detail
import com.komik.komik.service.utils.PreferenceManager
import com.komik.komik.service.utils.createNotificationChannel
import com.komik.komik.service.utils.formatChapterNumber
import com.komik.komik.service.utils.showNotification
import com.komik.komik.service.utils.typeSource
import com.komik.komik.ui.pages.read.ReadActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs

class DetailActivity : AppCompatActivity() {

    private lateinit var viewModel: DetailViewmodel
    private lateinit var binding: ActivityDetailBinding
    private lateinit var comicDao: ComicDao
    private lateinit var chapterDao: chapterDao
    private lateinit var genreDao: genreDao
    private lateinit var dbUtama: DatabaseRiwayats
    private lateinit var riwayatRelationDao: riwayatRelationDao
    private lateinit var libraryDao: LibraryDao

    private var isFavorite: Boolean = false
    private var isSelengkap: Boolean = false
    private var locations: String? = null
    private var sources: sourceType? = null
    private var urlResume: String? = null
    private var currentComic: Comic? = null

    private lateinit var adapterChapter: AdapterChapter

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewCompatPadding()
        initDatabase()
        initViewmodel()
        initRecycleview()

        val url = intent.getStringExtra(URL)
        val location = intent.getStringExtra(LOCATION)
        locations = location
        urlResume = url

        if (location?.contains("ikiru", ignoreCase = true) == true || url?.contains("ikiru", ignoreCase = true) == true) {
            Log.d("IKIRU_TRACE", "DetailActivity onCreate: location='$location', rawUrl='$url'")
        }

        createNotificationChannel("channel_detail", this)

        val source = typeSource.getSourceType(location.toString())
        sources = source

        viewModel.getChapters(source, url.toString())

        viewModel.loading.observe(this) { loading ->
            binding.loading.visibility = if (loading == true) View.VISIBLE else View.GONE
            binding.swipeRefreshLayout.isRefreshing = (loading == true)
        }

        observeDetail()

        viewModel.error.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        viewModel.updateChapter.observe(this) { info ->
            if (!info.isNullOrEmpty()) {
                Snackbar.make(binding.root, info, Snackbar.LENGTH_LONG).show()
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshData(source, url.toString())
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.back.setOnClickListener {
            finish()
        }

        // Expandable Synopsis with smooth Transition
        val synopsisToggleAction = View.OnClickListener {
            isSelengkap = !isSelengkap
            TransitionManager.beginDelayedTransition(binding.layoutSynopsis)
            if (isSelengkap) {
                binding.sinopsis.maxLines = Int.MAX_VALUE
                binding.selengkap.text = "Sembunyikan"
            } else {
                binding.sinopsis.maxLines = 3
                binding.selengkap.text = "Baca Selengkapnya"
            }
        }
        binding.selengkap.setOnClickListener(synopsisToggleAction)
        binding.sinopsis.setOnClickListener(synopsisToggleAction)
        binding.layoutSynopsis.setOnClickListener(synopsisToggleAction)

        // Quick Copy Judul Komik
        val copyTitleAction: (String) -> Unit = { rawTitle ->
            val cleanTitle = rawTitle.trim()
            if (cleanTitle.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Judul Komik", cleanTitle)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Judul komik disalin", Toast.LENGTH_SHORT).show()
            }
        }
        binding.title.setOnLongClickListener {
            copyTitleAction(binding.title.text.toString())
            true
        }
        binding.title.setOnClickListener {
            copyTitleAction(binding.title.text.toString())
        }
        binding.tvToolbarTitle.setOnLongClickListener {
            copyTitleAction(binding.tvToolbarTitle.text.toString())
            true
        }

        // Chapter Sort Toggle (runs in Dispatchers.Default)
        binding.btnSortChapter.setOnClickListener {
            val isDesc = viewModel.toggleSortOrder()
            binding.tvSortChapter.text = if (isDesc) "Terbaru" else "Terlama"
            binding.ivSortChapter.animate().rotationBy(180f).setDuration(200).start()
        }

        // Batch Download Action (VIP Exclusive)
        binding.btnBatchDownload?.setOnClickListener {
            val prefManager = PreferenceManager.getInstance(this)
            if (!prefManager.isVipOrAbove()) {
                com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Fitur VIP: Unduh Batch Chapter")
                    .setMessage("Fitur unduh banyak chapter sekaligus secara otomatis hanya tersedia untuk pengguna VIP / Moderator / Admin.\n\nUpgrade ke VIP untuk menikmati kemudahan mengunduh chapter tanpa batas!")
                    .setPositiveButton("Mengerti", null)
                    .show()
                return@setOnClickListener
            }

            val comic = currentComic
            if (comic == null) {
                Snackbar.make(binding.root, "Data komik belum selesai dimuat", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val chapters = viewModel.getAllLoadedChapters()
            if (chapters.isEmpty()) {
                Snackbar.make(binding.root, "Tidak ada chapter yang tersedia untuk diunduh", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val options = arrayOf(
                "Unduh 5 Chapter Berikutnya",
                "Unduh 10 Chapter Berikutnya",
                "Unduh Semua Chapter yang Belum Diunduh"
            )

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Unduh Batch Chapter")
                .setItems(options) { _, which ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val downloadDao = dbUtama.downloadDao()
                        val unreadOrUndownloaded = chapters.filter { item ->
                            val download = downloadDao.getDownloadByChapterIdSync(item.chapter.id)
                            val isCompleted = download != null && download.status == com.komik.komik.service.data.model.DownloadChapter.STATUS_COMPLETED
                            val isRunning = com.komik.komik.service.utils.ChapterDownloadManager.isChapterActive(item.chapter.id)
                            !isCompleted && !isRunning
                        }.sortedBy { it.chapter.numChapter }

                        val targets = when (which) {
                            0 -> unreadOrUndownloaded.take(5)
                            1 -> unreadOrUndownloaded.take(10)
                            else -> unreadOrUndownloaded
                        }.map { it.chapter }

                        withContext(Dispatchers.Main) {
                            if (targets.isEmpty()) {
                                Snackbar.make(binding.root, "Semua chapter terpilih sudah terunduh", Snackbar.LENGTH_SHORT).show()
                            } else {
                                com.komik.komik.service.utils.ChapterDownloadManager.downloadBatchChapters(this@DetailActivity, targets, comic)
                                Snackbar.make(binding.root, "Memulai unduhan ${targets.size} chapter di latar belakang", Snackbar.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        // Quick Jump / Search Nomor Chapter (runs in Dispatchers.Default)
        binding.etSearchChapter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                viewModel.filterChapters(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchChapter.text?.clear()
        }

        // Chunked Loading / Local Pagination on scroll
        binding.nestedScrollView.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                val child = v.getChildAt(0)
                if (child != null && scrollY >= (child.measuredHeight - v.measuredHeight - 450)) {
                    viewModel.loadNextChunk()
                }
            }
        )

        // Share Comic Button (Action bar and pinned toolbar)
        val triggerShare = {
            val title = currentComic?.title ?: binding.title.text.toString().ifEmpty { "Komik" }
            val comicType = binding.tipe.text.toString().ifEmpty { currentComic?.type ?: "" }
            val status = binding.status.text.toString().ifEmpty { currentComic?.status ?: "" }
            val url = urlResume ?: currentComic?.url ?: intent.getStringExtra(URL).orEmpty()
            val typeStatus = if (comicType.isNotEmpty() && status.isNotEmpty()) " ($comicType - $status)" else if (comicType.isNotEmpty()) " ($comicType)" else ""
            val shareText = "Baca $title$typeStatus di Komryuu: $url"

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                setType("text/plain")
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(shareIntent, "Bagikan Komik"))
        }
        binding.share.setOnClickListener { triggerShare() }
        binding.btnShareToolbar?.setOnClickListener { triggerShare() }

        // Filter Chip Daftar Chapter
        binding.chipFilterAll?.setOnClickListener {
            adapterChapter.setFilterType(AdapterChapter.FilterType.ALL)
            checkEmptyChapterState()
        }
        binding.chipFilterUnread?.setOnClickListener {
            adapterChapter.setFilterType(AdapterChapter.FilterType.UNREAD)
            checkEmptyChapterState()
        }
        binding.chipFilterOffline?.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val downloaded = dbUtama.downloadDao().getCompletedDownloads().map { it.chapterId }.toSet()
                withContext(Dispatchers.Main) {
                    adapterChapter.updateDownloadedIds(downloaded)
                    adapterChapter.setFilterType(AdapterChapter.FilterType.OFFLINE)
                    checkEmptyChapterState()
                }
            }
        }

        // Tombol Loncat Cepat ke Ch. Awal
        binding.btnJumpFirstChapter?.setOnClickListener {
            val count = adapterChapter.itemCount
            if (count > 0) {
                val isDesc = viewModel.isSortDescending()
                val targetPos = if (isDesc) count - 1 else 0
                binding.chapter.smoothScrollToPosition(targetPos)
            }
        }

        // Migrate Source Button
        binding.btnMigrate?.setOnClickListener {
            val comic = currentComic
            if (comic != null) {
                val bs = com.komik.komik.service.component.BottomSheetMigration.newInstance(
                    comicTitle = comic.title,
                    comicId = comic.id,
                    currentLocation = locations ?: comic.location,
                    currentUrl = urlResume ?: comic.url
                )
                bs.show(supportFragmentManager, com.komik.komik.service.component.BottomSheetMigration.TAG)
            } else {
                Snackbar.make(binding.root, "Data komik belum selesai dimuat", Snackbar.LENGTH_SHORT).show()
            }
        }

        // Report Issue Button (Action bar and pinned toolbar)
        val openReportDialog: (View) -> Unit = {
            val title = currentComic?.title ?: binding.title.text.toString().ifEmpty { "Komik" }
            val source = locations ?: currentComic?.location ?: sources?.name ?: ""
            val url = urlResume ?: currentComic?.url ?: intent.getStringExtra(URL).orEmpty()
            val bs = com.komik.komik.service.component.BottomSheetReport.newInstance(
                sourceName = source,
                comicTitle = title,
                chapterTitle = null,
                targetUrl = url,
                defaultType = "extension_error"
            )
            bs.show(supportFragmentManager, com.komik.komik.service.component.BottomSheetReport.TAG)
        }
        binding.btnReport?.setOnClickListener(openReportDialog)
        binding.btnReportToolbar?.setOnClickListener(openReportDialog)

        // Toolbar Collapsed Title
        binding.appBarLayout.addOnOffsetChangedListener { appBar, verticalOffset ->
            val totalScrollRange = appBar.totalScrollRange
            val isCollapsed = abs(verticalOffset) >= (totalScrollRange - 50)
            binding.tvToolbarTitle.visibility = if (isCollapsed) View.VISIBLE else View.GONE
        }
    }

    private fun observeDetail() {
        viewModel.dataChapterDetail.observe(this) { data ->
            currentComic = data
            binding.title.text = data.title
            binding.tvToolbarTitle.text = data.title
            binding.textView4.text = data.alternativeTitle ?: data.author
            // 1. Status Mapping & Fallback
            val rawStatus = data.status?.trim().orEmpty()
            val cleanStatus = when {
                rawStatus.equals("ongoing", ignoreCase = true) || rawStatus.equals("berjalan", ignoreCase = true) -> "Ongoing"
                rawStatus.equals("completed", ignoreCase = true) || rawStatus.equals("tamat", ignoreCase = true) || rawStatus.equals("selesai", ignoreCase = true) -> "Completed"
                rawStatus.isNotEmpty() -> rawStatus.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                else -> "-"
            }
            binding.status.text = cleanStatus
            when (cleanStatus.lowercase()) {
                "ongoing" -> binding.status.setTextColor(ContextCompat.getColor(this, R.color.text_emerald_400))
                "completed" -> binding.status.setTextColor(android.graphics.Color.parseColor("#60A5FA"))
                else -> binding.status.setTextColor(ContextCompat.getColor(this, R.color.gray_400))
            }

            // 2. Tipe Mapping & Fallback
            val rawType = data.type?.trim().orEmpty()
            val cleanType = when {
                rawType.equals("manhwa", ignoreCase = true) -> "Manhwa"
                rawType.equals("manga", ignoreCase = true) -> "Manga"
                rawType.equals("manhua", ignoreCase = true) -> "Manhua"
                rawType.equals("comic", ignoreCase = true) || rawType.equals("komik", ignoreCase = true) -> "Komik"
                rawType.isNotEmpty() -> rawType.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                else -> "Komik"
            }
            binding.tipe.text = cleanType

            binding.sinopsis.text = data.description

            // 3. Rating Mapping & Fallback
            val rawRating = data.rating?.trim().orEmpty()
            val isNumericRating = rawRating.isNotEmpty() &&
                    rawRating != "0" &&
                    rawRating != "0.0" &&
                    rawRating != "0.00" &&
                    rawRating != "-" &&
                    !rawRating.equals("n/a", ignoreCase = true)

            if (isNumericRating) {
                binding.tvRating.text = rawRating
                binding.ivStarRating.visibility = View.VISIBLE
            } else {
                binding.tvRating.text = "N/A"
                binding.ivStarRating.visibility = View.GONE
            }

            val isIkiru = sources == sourceType.ikiru || locations?.contains("ikiru", ignoreCase = true) == true
            if (isIkiru) {
                Log.d("IKIRU_TRACE", "DetailActivity observed comic: title='${data.title}', imageUrl='${data.imageUrl}', location='${data.location}'")
            }

            val coverModel: Any = if (data.localImageUri != null) {
                data.localImageUri
            } else if (isIkiru && data.imageUrl.isNotBlank()) {
                val headers = com.bumptech.glide.load.model.LazyHeaders.Builder()
                    .addHeader("Referer", client.getIkiruUrl())
                    .addHeader("Origin", client.getIkiruUrl().trimEnd('/'))
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .build()
                com.bumptech.glide.load.model.GlideUrl(data.imageUrl, headers)
            } else {
                data.imageUrl
            }

            Glide.with(this)
                .load(coverModel)
                .error(R.drawable.placeholder)
                .into(binding.banner)

            Glide.with(this)
                .load(coverModel)
                .error(R.drawable.placeholder)
                .into(binding.imageView3)

            checkLatestRead(data.id)
            checkPustaka(data.id)

            binding.button2.setOnClickListener {
                if (isFavorite) {
                    deletePustaka(data.id)
                } else {
                    insertPustaka(data.id, data.imageUrl, data.title)
                }
            }
            binding.btnBookmarkAction?.setOnClickListener {
                binding.button2.performClick()
            }
        }

        viewModel.dataGenre.observe(this) { data ->
            binding.genre.adapter = AdapterGenre(data) { genre ->
                Toast.makeText(this, "Genre: ${genre.genre}", Toast.LENGTH_SHORT).show()
            }
        }

        // Chunked / Paginated Chapters via ListAdapter + DiffUtil
        viewModel.visibleChapters.observe(this) { chapters ->
            CoroutineScope(Dispatchers.IO).launch {
                val downloaded = dbUtama.downloadDao().getCompletedDownloads().map { it.chapterId }.toSet()
                withContext(Dispatchers.Main) {
                    adapterChapter.setMasterList(chapters, downloaded)
                    checkEmptyChapterState()
                }
            }
        }

        // Skeleton Shimmer Loading Placeholder
        viewModel.isChapterLoading.observe(this) { isChapterLoading ->
            binding.layoutChapterSkeleton.visibility = if (isChapterLoading) View.VISIBLE else View.GONE
            if (isChapterLoading) {
                binding.layoutEmptyChapter.visibility = View.GONE
            }
        }

        // Loading more chunk indicator
        viewModel.isLoadingMore.observe(this) { isLoadingMore ->
            binding.loadingMoreChapter.visibility = if (isLoadingMore) View.VISIBLE else View.GONE
        }

        // Total Chapter Badge
        viewModel.totalChapterCount.observe(this) { total ->
            binding.textView12.text = "$total Chapter"
        }

        // Raw chapters for initial button setup
        viewModel.dataChapter.observe(this) { chapters ->
            currentComic?.let { comic ->
                setupInitialReadButton(comic, chapters)
            }
        }
    }

    private fun checkEmptyChapterState() {
        val isChapterLoading = viewModel.isChapterLoading.value == true
        binding.layoutEmptyChapter.visibility =
            if (adapterChapter.itemCount == 0 && !isChapterLoading) View.VISIBLE else View.GONE
    }

    private fun setupInitialReadButton(comic: Comic, chapters: List<ChapterwithComic>) {
        if (chapters.isEmpty()) return

        // Chapter 1 is the oldest chapter (smallest numChapter)
        val firstChapter = chapters.minByOrNull { it.chapter.numChapter } ?: chapters.last()

        // Only set fallback if user hasn't read yet
        CoroutineScope(Dispatchers.IO).launch {
            val history = riwayatRelationDao.getRiwayatByChapterId(comic.id)
            if (history == null) {
                withContext(Dispatchers.Main) {
                    binding.btnBaca.text = "Mulai Baca Ch. 1"
                    binding.baca.text = "Mulai Baca"

                    val launchFirst = View.OnClickListener {
                        val intent = Intent(this@DetailActivity, ReadActivity::class.java).apply {
                            putExtra(ReadActivity.EXTRA_URL, firstChapter.chapter.link)
                            putExtra(ReadActivity.EXTRA_TITLE, firstChapter.chapter.comicTitle)
                            putExtra(ReadActivity.EXTRA_TYPE, firstChapter.chapter.location)
                            putExtra(ReadActivity.EXTRA_ID, firstChapter.chapter.id)
                            putExtra("num", firstChapter.chapter.numChapter)
                            putExtra(ReadActivity.EXTRA_ID_COMIC, firstChapter.chapter.comicId)
                        }
                        startActivity(intent)
                    }

                    binding.btnBaca.setOnClickListener(launchFirst)
                    binding.baca.setOnClickListener(launchFirst)
                }
            }
        }
    }

    private fun checkLatestRead(comicId: Int) {
        riwayatRelationDao.getLastRiwayatsByComicId(comicId).observe(this) { historyDetail ->
            if (historyDetail != null) {
                val lastChapter = historyDetail.chapter
                adapterChapter.setLastReadChapterId(lastChapter.id)

                val displayTitle = if (lastChapter.numChapter > 0) {
                    "Ch. ${lastChapter.numChapter.formatChapterNumber()}"
                } else {
                    lastChapter.title
                }

                binding.btnBaca.text = "Lanjut Baca $displayTitle"
                binding.baca.text = "Lanjut $displayTitle"

                val continueReadingAction = View.OnClickListener {
                    val intent = Intent(this@DetailActivity, ReadActivity::class.java).apply {
                        putExtra(ReadActivity.EXTRA_URL, lastChapter.link)
                        putExtra(ReadActivity.EXTRA_TITLE, historyDetail.comic.title)
                        putExtra(ReadActivity.EXTRA_TYPE, lastChapter.location)
                        putExtra(ReadActivity.EXTRA_ID, lastChapter.id)
                        putExtra("num", lastChapter.numChapter)
                        putExtra(ReadActivity.EXTRA_ID_COMIC, historyDetail.comic.id)
                    }
                    startActivity(intent)
                }

                binding.btnBaca.setOnClickListener(continueReadingAction)
                binding.baca.setOnClickListener(continueReadingAction)
            }
        }
    }

    private fun checkPustaka(comicId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val data = libraryDao.getLibraryByComicId(comicId)
            withContext(Dispatchers.Main) {
                isFavorite = (data != null)
                updateBookmarkUI(isFavorite)
            }
        }
    }

    private fun insertPustaka(comicId: Int, imageUrl: String?, title: String) {
        CoroutineScope(Dispatchers.IO).launch {
            if (!imageUrl.isNullOrEmpty()) {
                libraryDao.insertLibrary(
                    Library(
                        comicId = comicId,
                        imageLokal = imageUrl
                    )
                )

                withContext(Dispatchers.Main) {
                    isFavorite = true
                    updateBookmarkUI(true)
                    showNotification(
                        this@DetailActivity,
                        "channel_detail",
                        102,
                        "Berhasil Menambahkan Komik",
                        "Komik $title berhasil ditambahkan ke Pustaka"
                    )
                    Toast.makeText(this@DetailActivity, "Ditambahkan ke Pustaka", Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.root, "Gagal memuat gambar komik", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deletePustaka(comicId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            libraryDao.deleteLibrary(comicId)
            withContext(Dispatchers.Main) {
                isFavorite = false
                updateBookmarkUI(false)
                Toast.makeText(this@DetailActivity, "Dihapus dari Pustaka", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateBookmarkUI(isBookmarked: Boolean) {
        if (isBookmarked) {
            binding.button2.setImageResource(R.drawable.bookmark_simple_fill)
            binding.button2.setColorFilter(ContextCompat.getColor(this, R.color.brand))
            binding.tvBookmarkLabel?.text = "Tersimpan"
            binding.tvBookmarkLabel?.setTextColor(ContextCompat.getColor(this, R.color.brand))
        } else {
            binding.button2.setImageResource(R.drawable.bookmark_simple)
            binding.button2.setColorFilter(ContextCompat.getColor(this, R.color.white))
            binding.tvBookmarkLabel?.text = "Pustaka"
            binding.tvBookmarkLabel?.setTextColor(ContextCompat.getColor(this, R.color.grey_300))
        }
    }

    private fun initViewmodel() {
        val repository = Detail(
            comicDao = comicDao,
            client = client,
            this
        )

        viewModel = ViewModelProvider(
            this,
            Providerviewmodel(repository, comicDao, chapterDao, genreDao)
        )[DetailViewmodel::class.java]
    }

    private fun initDatabase() {
        try {
            dbUtama = DatabaseRiwayats.getDatabase(this)
            riwayatRelationDao = dbUtama.riwayatdao()
            comicDao = dbUtama.comicdao()
            chapterDao = dbUtama.chapterdao()
            genreDao = dbUtama.genreDao()
            libraryDao = dbUtama.libraryDao()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "initDatabase: ${e.localizedMessage}")
        }
    }

    private fun viewCompatPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        window.navigationBarColor = ContextCompat.getColor(this, R.color.bottom_navbar)

        WindowCompat.getInsetsController(window, window.decorView).let { controller ->
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    private fun initRecycleview() {
        binding.genre.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.chapter.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        adapterChapter = AdapterChapter(emptyList()) { chapterWithComic ->
            checkLatestRead(chapterWithComic.chapter.comicId)
        }
        binding.chapter.adapter = adapterChapter
    }

    companion object {
        const val TAG = "DetailActivity"
        const val URL = "url"
        const val LOCATION = "location"
    }
}