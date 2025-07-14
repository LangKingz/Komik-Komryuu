package com.komik.komik.ui.pages.source

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.komik.komik.R
import com.komik.komik.databinding.ActivitySourceBinding
import com.komik.komik.service.adapter.AdapterManhwa
import com.komik.komik.service.client
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.parser.comic.SourceParser
import com.komik.komik.service.parser.model.getListUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class sourceActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySourceBinding
    private lateinit var viewmodel: sourceViewmodel

    private lateinit var adapterManhwa: AdapterManhwa

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySourceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapterManhwa = AdapterManhwa(mutableListOf())

        window.navigationBarColor = resources.getColor(R.color.bottom_navbar)

        WindowCompat.getInsetsController(window, window.decorView).let { controller ->
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }
        viewmodel = ViewModelProvider(this).get(sourceViewmodel::class)

        binding.listSource.layoutManager = GridLayoutManager(this, 3)

        val url = intent.getStringExtra(URL)
        val location = intent.getStringExtra(location)

        binding.appbar.title = location
        binding.appbar.subtitle = url

        when (location) {
            "ikiru" -> viewmodel.getSource(sourceType.ikiru)
            "comicscan" -> viewmodel.getSource(sourceType.comicscan)
            "shinigami" -> viewmodel.getSource(sourceType.shinigami)
            "komikcast" -> viewmodel.getSource(sourceType.komikcast)
            "kiryuu" -> viewmodel.getSource(sourceType.kiryuu)
            "komiku" -> viewmodel.getSource(sourceType.komiku)
            else -> viewmodel.getSource(sourceType.ikiru)
        }


        binding.appbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.webview -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    true
                }
                else -> false}

        }


        viewmodel.source.observe(this) { source ->
            Log.d(TAG, "onCreate: $source")
            adapterManhwa.addData(source)
            binding.listSource.adapter = adapterManhwa
        }

        viewmodel.loading.observe(this) { loading ->
            if (loading) {
                binding.loading.visibility = View.VISIBLE
            } else {
                binding.loading.visibility = View.GONE
            }
        }

        binding.appbar.setNavigationOnClickListener {
            finish()
        }

        viewmodel.error.observe(this) { error ->
            if (error != null) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
                    .setAction("action") {
                        finish()
                    }
                    .show()
            }
        }


    }

    fun scrapeTest  (url: String) {

        CoroutineScope(Dispatchers.IO).launch {

            val html = client.Ikiru.getHtml("https://kiryuu02.com/manga/?page=1&order=update")
            val document = Jsoup.parse(html)

            val pageData = SourceParser.parseKiryuu(document)


            Log.d(TAG, "scrapeTest: ${pageData.size}")

//            withContext(Dispatchers.Main) {
//                binding.listSource.adapter = AdapterManhwa(pageData as MutableList<getListUpdate>)
//            }
        }

    }

    companion object {
        const val TAG = "sourceActivity"
        const val URL = "url"
        const val location = "location"
    }
}