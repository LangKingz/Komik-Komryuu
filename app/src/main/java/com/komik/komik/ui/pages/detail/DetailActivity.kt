package com.komik.komik.ui.pages.detail

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
import com.komik.komik.service.data.dao.ComicDao
import com.komik.komik.service.data.dao.chapterDao
import com.komik.komik.service.data.dao.genreDao
import com.komik.komik.service.data.dao.riwayatRelationDao
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.dao.LibraryDao
import com.komik.komik.service.data.model.Chapter
import com.komik.komik.service.data.model.Comic
import com.komik.komik.service.data.model.Genre
import com.komik.komik.service.data.model.Library
import com.komik.komik.service.data.model.Riwayats
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.repository.Detail
import com.komik.komik.service.utils.downloadAndSaveImage
import com.komik.komik.service.utils.genereteId
import com.komik.komik.ui.pages.read.ReadActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

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
    private var isSelengkap: Boolean? = false
    private var locations: String? = null
    private var sources: sourceType? = null
    private var urlResume: String? = null
    private lateinit var adapterChapter: AdapterChapter

    override fun onCreate(savedInstanceState: Bundle?) {
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

        val title = binding.title.text

        val source = when (location) {
            "ikiru" -> sourceType.ikiru
            "komikcast" -> sourceType.komikcast
            "kiryuu" -> sourceType.kiryuu
            "komiku" -> sourceType.komiku
            "comicscan" -> sourceType.comicscan
            "shinigami" -> sourceType.shinigami
            "manhwaindo" -> sourceType.manhwaindo
            "batoto-all" -> sourceType.batoto_all
            "dd" -> sourceType.doujindesu
            else -> sourceType.ikiru
        }

        sources = source

        viewModel.getChapters(source,url.toString())

        viewModel.loading.observe(this){loading ->
            if(loading == true){
                binding.loading.visibility = View.VISIBLE
                binding.nestedScrollView.visibility = View.GONE
                binding.swipeRefreshLayout.isRefreshing = loading
            }else{
                binding.loading.visibility = View.GONE
                binding.nestedScrollView.visibility = View.VISIBLE
                binding.swipeRefreshLayout.isRefreshing = loading
            }
        }

        detail()

        viewModel.error.observe(this) { error ->
            if (error != null) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            }
        }



        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = false
            viewModel.refreshData(source, url.toString())
        }


        binding.back.setOnClickListener {
            finish()
        }

        binding.selengkap.setOnClickListener {
            isSelengkap = !isSelengkap!!
            if (isSelengkap == true) {
                binding.sinopsis.maxLines = Int.MAX_VALUE
                binding.selengkap.text = "Sembunyikan"
            } else {
                binding.sinopsis.maxLines = 3
                binding.selengkap.text = "Selengkapnya"
            }
        }

    }

    private fun detail(){
        viewModel.dataChapterDetail.observe(this){data->

            CheckLatestRead(data.id)


            checkPustaka(data.id)

            Glide.with(this@DetailActivity)
                .load(data.localImageUri)
                .error(R.drawable.placeholder)
                .into(binding.banner)

            Glide.with(this@DetailActivity)
                .load(data.localImageUri)
                .error(R.drawable.placeholder)
                .into(binding.imageView3)

            binding.title.text = data.title
            binding.textView4.text = data.alternativeTitle
            binding.status.text = data.status
            binding.sinopsis.text = data.description

            binding.button2.setOnClickListener {
                if (isFavorite) {
                    deletePustaka(data.id)
                } else {
                    insertPustaka(data.id, data.imageUrl)
                }
            }
        }

        viewModel.dataGenre.observe(this){data ->
            binding.genre.adapter = AdapterGenre(data)
        }

        viewModel.dataChapter.observe(this){chapter ->
            binding.textView12.text = "${chapter.size} Chapter"
            adapterChapter.updateList(chapter)
            binding.chapter.adapter = adapterChapter

            binding.btnBaca.setOnClickListener {
                val intent = Intent(this@DetailActivity, ReadActivity::class.java)
                intent.putExtra(ReadActivity.EXTRA_URL, chapter.first().chapter.link)
                intent.putExtra(ReadActivity.EXTRA_TITLE, chapter.first().chapter.title)
                intent.putExtra(ReadActivity.EXTRA_TYPE, chapter.first().chapter.location)
                intent.putExtra(ReadActivity.EXTRA_ID, chapter.first().chapter.id)
                intent.putExtra("num", chapter.first().chapter.numChapter)
                startActivity(intent)
            }

        }
    }


    private fun insertPustaka(chapterId: Int, imageUrl: String? = null) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                val id = genereteId()
                val dataPathImage =
                    downloadAndSaveImage(this@DetailActivity, imageUrl.toString(), id.toLong())

                if (dataPathImage != null) {
                    libraryDao.insertLibrary(
                        Library(
                            comicId = chapterId,
                            imageLokal = dataPathImage
                        )
                    )

                    withContext(Dispatchers.Main) {
                        isFavorite = true
                        binding.button2.setImageResource(R.drawable.bookmark_simple_fill)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Snackbar.make(binding.root, "Gagal Memuat Gambar", Snackbar.LENGTH_LONG)
                            .show()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "insertPustaka: ${e.localizedMessage}")
        }
    }

    private fun deletePustaka(chapterId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            libraryDao.deleteLibrary(chapterId)

            withContext(Dispatchers.Main) {
                isFavorite = false
                binding.button2.setImageResource(R.drawable.bookmark_simple)
            }
        }
    }

    private fun checkPustaka(chapterId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val data = libraryDao.getLibraryByComicId(chapterId)
            if (data != null) {
                isFavorite = true
                binding.button2.setImageResource(R.drawable.bookmark_simple_fill)
            } else {
                isFavorite = false
                binding.button2.setImageResource(R.drawable.bookmark_simple)
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun CheckLatestRead(comidId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val data = riwayatRelationDao.getLastRiwayatsByComicId(comidId)

            withContext(Dispatchers.Main) {
                data.observe(this@DetailActivity) { chapter ->
                    if (chapter != null) {
                        Log.d(TAG, "CheckLatestRead: ${chapter.riwayat.readAt}")
                        binding.btnBaca.text = "Lanjut Baca ${chapter.chapter.title}"
                        binding.btnBaca.setOnClickListener {
                            val intent = Intent(this@DetailActivity, ReadActivity::class.java)
                            intent.putExtra(ReadActivity.EXTRA_URL, chapter.chapter.link)
                            intent.putExtra(ReadActivity.EXTRA_TITLE, chapter.comic.title)
                            intent.putExtra(ReadActivity.EXTRA_TYPE, chapter.chapter.location)
                            intent.putExtra(ReadActivity.EXTRA_ID, chapter.chapter.id)
                            intent.putExtra("num", chapter.chapter.numChapter)
                            startActivity(intent)
                        }
                    }
                }
            }
        }
    }


    private fun initViewmodel() {
        val repository = Detail(
            comicDao = comicDao,
            client = client,
            this@DetailActivity
        )

        viewModel =
            ViewModelProvider(
                this,
                Providerviewmodel(repository, comicDao, chapterDao,genreDao)
            )[DetailViewmodel::class]

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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        window.navigationBarColor = resources.getColor(R.color.bottom_navbar)

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

        adapterChapter = AdapterChapter(mutableListOf())
    }

    companion object {
        const val TAG = "DetailActivity"
        const val URL = "url"
        const val LOCATION = "location"
    }
}