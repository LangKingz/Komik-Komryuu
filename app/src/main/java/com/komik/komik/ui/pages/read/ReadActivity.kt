package com.komik.komik.ui.pages.read

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.google.android.material.snackbar.Snackbar
import com.komik.komik.R
import com.komik.komik.databinding.ActivityReadBinding
import com.komik.komik.service.adapter.AdapterImage
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.dao.chapterDao
import com.komik.komik.service.data.dao.riwayatRelationDao
import com.komik.komik.service.data.model.Chapter
import com.komik.komik.service.data.model.Riwayats
import com.komik.komik.service.enumclass.sourceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ReadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReadBinding
    private lateinit var viewmodel: ReadViewmodel

    private var isTap: Boolean = false

    private lateinit var db: DatabaseRiwayats
    private lateinit var dao: chapterDao
    private lateinit var riwayats: riwayatRelationDao

    private var currentId: Int = 0

    private var currentChapter: Double = 0.0

    private var isLoadingNextChapter = false
    private var isLoadingPrevChapter = false

    private var pageRead :Int = 1
    private var totalPage :Int = 1


    private lateinit var AllChapter : List<Chapter>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityReadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        initRecyclerView()

        viewmodel = ViewModelProvider(this)[ReadViewmodel::class]

        val url = intent.getStringExtra(EXTRA_URL).toString()
        val title = intent.getStringExtra(EXTRA_TITLE)
        val type = intent.getStringExtra(EXTRA_TYPE).toString()
        val id = intent.getStringExtra(EXTRA_ID)
        val num = intent.getDoubleExtra("num", 0.0)
        val idComic = intent.getIntExtra(EXTRA_ID_COMIC, 0)

        val lastPageread = intent.getIntExtra("pageRead", 0)

        if(lastPageread != 0){
            binding.listImage.scrollToPosition(lastPageread)
        }

        Log.d(TAG, "onCreate: $idComic $num")

        currentChapter = num
        currentId = idComic

        val pref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val pindahChapterOtomatis = pref.getBoolean("pindah",true)


        val location = when (type) {
            "ikiru" -> sourceType.ikiru
            "comicscan" -> sourceType.comicscan
            "shinigami" -> sourceType.shinigami
            "komikcast" -> sourceType.komikcast
            "kiryuu" -> sourceType.kiryuu
            "komiku" -> sourceType.komiku
            "manhwaindo" -> sourceType.manhwaindo
            "dd" -> sourceType.doujindesu
            "batoto-all" -> sourceType.batoto_all
            else -> sourceType.ikiru
        }

        setUpListener(location,type,title,idComic)

        viewmodel.setImage(location, url)

        viewmodel.image.observe(this) { list ->
            binding.listImage.adapter = AdapterImage(list, location, url)
            binding.slider.valueFrom = 0f
            binding.slider.valueTo = list.size.toFloat()
            binding.slider.setValues(0f) // Set nilai awal menjadi 0
            binding.slider.stepSize = 1.0f
        }

        binding.judulManhwa.text = "$title - Chapter ${num.toInt()}"

        binding.tapDetector.setOnClickListener {
            isTap = !isTap
            if (isTap) {
                binding.chapterControls.visibility = View.VISIBLE
            } else {
                binding.chapterControls.visibility = View.GONE
            }
        }

        binding.slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val posisi = value.toInt()
                binding.listImage.scrollToPosition(posisi)
                binding.listImage.smoothScrollToPosition(posisi)
            }
        }

        binding.listImage.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItem = layoutManager.itemCount
                val lastVisibleIPosition = layoutManager.findLastVisibleItemPosition()

                val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()

                pageRead = firstVisiblePosition + 1
                totalPage = totalItem

                binding.lis3.text = "${firstVisiblePosition + 1}/${totalItem}"

                if (binding.slider.values.first() != firstVisiblePosition.toFloat()) {
                    binding.slider.setValues(firstVisiblePosition.toFloat())
                }

                if(pindahChapterOtomatis){
                    CoroutineScope(Dispatchers.IO).launch {
                        val allChapter = dao.getChapterForRead(title.toString(), type)
                        Log.d(TAG, "onCreate: $allChapter")

                        withContext(Dispatchers.Main) {
                            if (dy > 0 && !isLoadingNextChapter) {
                                //jika tidak bisa discroll kebawah
                                if (!recyclerView.canScrollVertically(1)) {
                                    isLoadingNextChapter = true
                                    nextChapterButtonListener(allChapter, location,idComic)
                                }
                            }

                            if (dy < 0 && !isLoadingPrevChapter) {
                                //jika sudah tidak bisa discroll keatas
                                if (!recyclerView.canScrollVertically(-1)) {
                                    isLoadingPrevChapter = true
                                    PrevChapterListerner(location,allChapter,idComic)
                                }
                            }
                        }

                    }
                }

            }
        })



    }

    suspend fun insertRiwayat(chapter_id:Int , comicId:Int){
        val data = riwayats.getRiwayatByChapterId(comicId)
        if(data == null){
            riwayats.insertRiwayatRelasi(
                Riwayats(
                    chapterId = chapter_id,
                    comicId = comicId
                )
            )
        }else{
            riwayats.updateRiwayatRelasi(
                data.copy(
                    chapterId = chapter_id,
                    comicId = comicId,
                    readAt = System.currentTimeMillis()
                )
            )
        }
    }

    override fun onPause() {
        super.onPause()
        simpanProgress()
    }

    private fun simpanProgress(){
        if(currentId != null){
            CoroutineScope(Dispatchers.IO).launch {
                riwayats.updateProgress(currentId, pageRead, totalPage)
                Log.d(TAG, "simpanProgress: $currentId $pageRead $totalPage")
            }
        }
    }

    private fun setUpListener(location: sourceType, type: String, title: String?,comicId: Int){
        CoroutineScope(Dispatchers.IO).launch {
            val allChapter = dao.getChapterForRead(title.toString(), type)
            Log.d(TAG, "onCreate: $allChapter")

            withContext(Dispatchers.Main) {
                binding.btnNextChapter.setOnClickListener {
                    nextChapterButtonListener(allChapter, location,comicId)
                }

                binding.btnPrevChapter.setOnClickListener {
                    PrevChapterListerner(location,allChapter,comicId)
                }
            }

        }
    }


    fun initRecyclerView() {
        binding.listImage.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        db = DatabaseRiwayats.getDatabase(this)
        dao = db.chapterdao()
        riwayats = db.riwayatdao()
    }

    fun initView() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }
    }

    override fun onResume() {
        super.onResume()

        val pref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val fullscreen = pref.getBoolean("fullscreen", true)

        if (fullscreen) {
            hideUi()
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

    }

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

    private fun nextChapterButtonListener(allChapter: List<Chapter>, location: sourceType,ComidID: Int){
        val nextChapter = nextChapter(currentChapter, allChapter)
        if (nextChapter != null) {

            CoroutineScope(Dispatchers.IO).launch {
                insertRiwayat(nextChapter.id, ComidID)
                withContext(Dispatchers.Main){
                    viewmodel.setImage(location, nextChapter.link)
                    currentChapter = nextChapter.numChapter
                    Toast.makeText(this@ReadActivity, nextChapter.title, Toast.LENGTH_SHORT).show()
                    isLoadingNextChapter = false
                    isLoadingPrevChapter = false
                    binding.judulManhwa.text = "${nextChapter.comicTitle} - ${nextChapter.title}"
                }
            }


        } else {
            Snackbar.make(
                binding.root,
                "Tidak ada chapter lagi",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun PrevChapterListerner(location: sourceType, allChapter: List<Chapter>,ComidID: Int){
        val prev = getPreviousChapter(currentChapter, allChapter)
        if (prev != null) {
            CoroutineScope(Dispatchers.IO).launch {
                insertRiwayat(prev.id, ComidID)
                withContext(Dispatchers.Main){
                    viewmodel.setImage(location, prev.link)
                    currentChapter = prev.numChapter
                    Toast.makeText(this@ReadActivity, prev.title, Toast.LENGTH_SHORT).show()
                    isLoadingPrevChapter = false
                    isLoadingNextChapter = false
                    binding.judulManhwa.text = "${prev.comicTitle} - ${prev.title}"
                }
            }
        } else {
            Snackbar.make(
                binding.root,
                "Tidak ada chapter lagi",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun nextChapter(currentChapter: Double, allChapter: List<Chapter>): Chapter? {
//         Ini bakal nyari chapter di dalam list allChapters yang punya numChapter sama dengan currentChapterNumber yang kita kasih. Dia bakal ngembaliin indeks (posisi) chapter itu di list. Kalau nggak ketemu, dia ngembaliin -1
        val currentIndex = allChapter.indexOfFirst { it.numChapter == currentChapter }
        Log.d(TAG, "nextChapter: $currentChapter, $currentIndex")
//        jika tidak ada
        if (currentIndex == -1) {
            return null
        }
//        cek apakah currentIndex kalau ditambah 1 masi lebih kecil dari semua chapter
        if (currentIndex + 1 < allChapter.size) {
            return allChapter[currentIndex + 1]
        } else {
//            latest chapter
            return null
        }
    }

    private fun getPreviousChapter(currentChapter: Double, allChapter: List<Chapter>): Chapter? {
        val currentIndex = allChapter.indexOfFirst { it.numChapter == currentChapter }

        if (currentIndex == -1) {
            return null
        }
        if (currentIndex - 1 >= 0) {
            return allChapter[currentIndex - 1]
        } else {
            return null
        }
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