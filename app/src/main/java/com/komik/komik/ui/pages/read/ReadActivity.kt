package com.komik.komik.ui.pages.read

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.komik.komik.R
import com.komik.komik.databinding.ActivityReadBinding
import com.komik.komik.service.adapter.AdapterImage
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.dao.chapterDao
import com.komik.komik.service.enumclass.sourceType


class ReadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReadBinding
    private lateinit var viewmodel: ReadViewmodel

    private var isTap: Boolean = false

    private lateinit var db: DatabaseRiwayats
    private lateinit var dao: chapterDao

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

        when (type) {
            "ikiru" -> viewmodel.setImage(sourceType.ikiru, url)
            "comicscan" -> viewmodel.setImage(sourceType.comicscan, url)
            "shinigami" -> viewmodel.setImage(sourceType.shinigami, url)
            "komikcast" -> viewmodel.setImage(sourceType.komikcast, url)
            "kiryuu" -> viewmodel.setImage(sourceType.kiryuu, url)
            "komiku" -> viewmodel.setImage(sourceType.komiku, url)
            "manhwaindo" -> viewmodel.setImage(sourceType.manhwaindo, url)
            else -> viewmodel.setImage(sourceType.ikiru, url)
        }

        viewmodel.image.observe(this) { list ->
            binding.listImage.adapter = AdapterImage(list)
            binding.slider.valueFrom = 0f
            binding.slider.valueTo = list.size.toFloat()
            binding.slider.setValues(0f) // Set nilai awal menjadi 0
            binding.slider.stepSize = 1.0f
        }

        binding.judulManhwa.text = title

        binding.tapDetector.setOnClickListener {
            isTap = !isTap
            if (isTap) {
                binding.chapterControls.visibility = View.VISIBLE
            } else {
                binding.chapterControls.visibility = View.GONE
            }
        }

        binding.slider.addOnChangeListener{ _, value,fromUser ->
            if (fromUser){
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

                binding.lis3.text = "${firstVisiblePosition + 1}/${totalItem}"

                // 🛠 Update posisi RangeSlider saat user scroll manual
                if (binding.slider.values.first() != firstVisiblePosition.toFloat()) {
                    binding.slider.setValues(firstVisiblePosition.toFloat())
                }
            }
        })

    }

    private fun InitDetailChapter(){

    }

    private fun saveReadingProgress() {
        // Ambil posisi gambar yang terlihat saat ini
        val layoutManager = binding.listImage.layoutManager as LinearLayoutManager
        val lastVisiblePosition =
            layoutManager.findFirstVisibleItemPosition() // Gunakan first visible untuk penanda
        val totalItem = layoutManager.itemCount

        if (totalItem > 0 && lastVisiblePosition != RecyclerView.NO_POSITION) {
            val progress = "${lastVisiblePosition + 1}/${totalItem}"
            val chapterId =
                intent.getStringExtra(EXTRA_ID) // Asumsi EXTRA_ID adalah unik untuk chapter


            Log.d(TAG, "Progress saved for chapter $chapterId at page ${lastVisiblePosition + 1}")
        }
    }

    override fun onPause() {
        super.onPause()
        saveReadingProgress()
    }

    override fun onStop() {
        super.onStop()
        saveReadingProgress()
    }

    fun initRecyclerView() {
        binding.listImage.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        db = DatabaseRiwayats.getDatabase(this)
        dao = db.chapterdao()
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
        hideUi()
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

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_ID = "extra_id"

        const val TAG = "ReadActivity"
    }
}