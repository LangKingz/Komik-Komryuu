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
import com.komik.komik.service.data.model.Chapter
import com.komik.komik.service.data.model.Comic
import com.komik.komik.service.data.model.Genre
import com.komik.komik.service.data.model.Library
import com.komik.komik.service.data.model.Riwayats
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.repository.Detail
import com.komik.komik.ui.pages.read.ReadActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class DetailActivity : AppCompatActivity() {

    private lateinit var viewModel: DetailViewmodel
    private lateinit var binding: ActivityDetailBinding

    private lateinit var comicDao: ComicDao
    private lateinit var chapterDao: chapterDao
    private lateinit var genreDao: genreDao
    private lateinit var dbUtama : DatabaseRiwayats
    private lateinit var riwayatRelationDao : riwayatRelationDao

    private var isFavorite: Boolean? = false

    private var isSelengkap: Boolean? = false

    private var locations : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        inisialize
        viewCompatPadding()
        initDatabase()
        initViewmodel()
        initRecycleview()

        val url = intent.getStringExtra(URL)
        val location = intent.getStringExtra(LOCATION)
        locations = location

//        Snackbar.make(binding.root, location.toString(), Snackbar.LENGTH_LONG).show()

        val source = when (location) {
            "ikiru" -> sourceType.ikiru
            "komikcast" -> sourceType.komikcast
            "kiryuu" -> sourceType.kiryuu
            "komiku" -> sourceType.komiku
            "comicscan" -> sourceType.comicscan
            "shinigami" -> sourceType.shinigami
            else -> sourceType.ikiru
        }



        viewModel.dataDetail.observe(this) { data ->
            if (data != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    detail(url.toString(), data)
                }
            }
        }

        viewModel.loading.observe(this) { loading ->
            if (loading) {
                binding.loading.visibility = View.VISIBLE
            } else {
                binding.loading.visibility = View.GONE
                binding.nestedScrollView.visibility = View.VISIBLE
            }
        }

        viewModel.error.observe(this) { error ->
            if (error != null) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            }
        }

        loadData(url.toString(),source)
    }

    fun loadData(url: String,type: sourceType){
        CoroutineScope(Dispatchers.IO).launch {
            binding.loading.visibility = View.VISIBLE
            val dataDetail = comicDao.getComicByUrl(url)


            if (dataDetail != null) {
                detail(url, dataDetail)
                binding.nestedScrollView.visibility = View.VISIBLE
            } else {
                withContext(Dispatchers.Main) {
                    viewModel.getDetail(type, url)
                }

            }

        }
    }

//    fun insertPustaka(chapterId: Int){
//        CoroutineScope(Dispatchers.IO).launch {
//            comicDao.insertLibrary(
//                Library(
//                    comicId = chapterId
//                )
//            )
//        }
//    }

//    fun deletePustaka(chapterId: Int){
//        CoroutineScope(Dispatchers.IO).launch {
//            comicDao.deleteLibrary(chapterId)
//        }
//    }

//    fun checkPustaka(chapterId: Int){
//        CoroutineScope(Dispatchers.IO).launch {
//            val data = comicDao.getLibraryByComicId(chapterId)
//            if (data != null) {
//                isFavorite = true
//                binding.button2.setImageResource(R.drawable.bookmark_simple_fill)
//            } else {
//                isFavorite = false
//                binding.button2.setImageResource(R.drawable.bookmark_simple)
//            }
//        }
//    }



    suspend fun detail(url: String, data: Comic) {
        val genre = genreDao.getGenreByComicTitle(data.title)
        val chapter = chapterDao.getChaptersForComic(data.title)
        Log.d(TAG, "detail: $chapter")
        withContext(Dispatchers.Main) {

//            checkPustaka(data.id)

            binding.loading.visibility = View.GONE

            binding.back.setOnClickListener {
                finish()
            }

            Glide.with(this@DetailActivity)
                .load(data.imageUrl)
                .error(R.drawable.placeholder)
                .into(binding.banner)

            Glide.with(this@DetailActivity)
                .load(data.imageUrl)
                .error(R.drawable.placeholder)
                .into(binding.imageView3)

            binding.title.text = data.title
            binding.textView4.text = data.alternativeTitle
            binding.status.text = data.status
            binding.sinopsis.text = data.description

            binding.textView12.text = "${chapter.size} Chapter"

            binding.genre.adapter = AdapterGenre(genre)
            binding.chapter.adapter = AdapterChapter(chapter) { list ->
                if (data != null) {
//                    insertRiwayat(
//                        chapterId = list.id,
//                        comicId = data.id
//                    )
                }else{
                    Snackbar.make(binding.root, "Data tidak ditemukan", Snackbar.LENGTH_LONG)
                        .setAction("Refresh"){
                            val source = when (locations) {
                                "ikiru" -> sourceType.ikiru
                                "komikcast" -> sourceType.komikcast
                                "kiryuu" -> sourceType.kiryuu
                                "komiku" -> sourceType.komiku
                                "comicscan" -> sourceType.comicscan
                                "shinigami" -> sourceType.shinigami
                                else -> sourceType.ikiru
                            }
                            loadData(url,source)
                        }
                }

                val intent = Intent(this@DetailActivity, ReadActivity::class.java)
                intent.putExtra(ReadActivity.EXTRA_URL, list.link)
                intent.putExtra(ReadActivity.EXTRA_TITLE, list.title)
                intent.putExtra(ReadActivity.EXTRA_TYPE, list.location)
                intent.putExtra(ReadActivity.EXTRA_ID, list.id)
                startActivity(intent)

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

            binding.btnBaca.setOnClickListener {
                val intent = Intent(this@DetailActivity, ReadActivity::class.java)
                intent.putExtra(ReadActivity.EXTRA_URL, chapter.first().link)
                startActivity(intent)
            }

//            binding.button2.setOnClickListener {
//                if (isFavorite == true) {
//                    isFavorite = false
//                    deletePustaka(data.id)
//                } else {
//                    isFavorite = true
//                    insertPustaka(data.id)
//                }
//            }

        }
    }

    fun insertRiwayat(chapterId: Int,comicId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val data = riwayatRelationDao.getRiwayatByChapterId(comicId)
            if (data == null) {
                riwayatRelationDao.insertRiwayatRelasi(
                    Riwayats(
                        chapterId = chapterId,
                        comicId = comicId
                    )
                )
            } else {
                riwayatRelationDao.updateRiwayatRelasi(
                    data.copy(
                        chapterId = chapterId,
                        comicId = comicId
                    )
                )
            }
        }
    }



    fun initViewmodel() {
        val repository = Detail(
            comicDao = comicDao,
            client = client
        )

        viewModel = ViewModelProvider(this, Providerviewmodel(repository))[DetailViewmodel::class]

    }

    fun initDatabase() {
        dbUtama = DatabaseRiwayats.getDatabase(this)
        riwayatRelationDao = dbUtama.riwayatdao()
        comicDao = dbUtama.comicdao()
        chapterDao = dbUtama.chapterdao()
        genreDao = dbUtama.genreDao()
    }

    fun viewCompatPadding() {
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

    fun initRecycleview() {
        binding.genre.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.chapter.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

    }

    fun testScrape() {
        CoroutineScope(Dispatchers.IO).launch {

            val html = client.Ikiru.getHtml("https://id.ikiru.wtf/manga/golden-print/")
            val document = Jsoup.parse(html)

            val bodyClasses = document.body().className().split(" ")
            val mangaId =
                bodyClasses.firstOrNull { it.startsWith("postid-") }?.removePrefix("postid-")

            val idparser =
                client.Ikiru.getHtml("https://id.ikiru.wtf/ajax-call?manga_id=${mangaId}&page=1&action=chapter_list")
            val document2 = Jsoup.parse(idparser)

            val title = document.select(".flex h1.text-2xl")[0].text()
            val imageUrl = document.select(".contents img").attr("src")
            val deskripsitable = document.select("div").attr("itemprop", "description").text()
            val author = document.select("span.komik_info-content-info").text()

            val rating = document.select("span.to-accent-2").text()
            val type = document.select("p.ml-1").text()
            val alternative = document.select("div.text-text").text()


            val chapters =
                document2.select("div#chapter-list div.cursor-pointer a.cursor-pointer").map {
                    Chapter(
                        title = it.select("span").text(),
                        link = it.attr("href"),
                        comicTitle = title,
                        date = it.select("time").text()
                    )
                }


            val genres = document.select("span.text-inherit")
                .map {
                    Genre(
                        comicTitle = title,
                        genre = it.text()
                    )
                }

            Log.d(TAG, "genres: $genres")
            Log.d(TAG, "chapters: ${chapters}")

            Log.d(TAG, "title: ${title}")
            Log.d(TAG, "imageUrl: ${imageUrl}")
            Log.d(TAG, "deskripsitable: ${deskripsitable}")
            Log.d(TAG, "author: ${author}")
            Log.d(TAG, "id: ${mangaId}")

            Log.d(TAG, "rating: ${rating}")
            Log.d(TAG, "type: ${type}")
            Log.d(TAG, "alternative: ${alternative}")


        }
    }


    companion object {
        const val TAG = "DetailActivity"
        const val URL = "url"
        const val LOCATION = "location"
    }
}