package com.komik.komik.ui.pages.source

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.SearchView
import android.widget.Toast
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
import com.komik.komik.service.component.BottomsheetInfo
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.parser.comic.SourceParser
import com.komik.komik.service.parser.model.getListUpdate
import com.komik.komik.service.utils.typeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class sourceActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySourceBinding
    private lateinit var viewmodel: sourceViewmodel

    private lateinit var adapterManhwa: AdapterManhwa

    private lateinit var locations : String

    var urlSource: String = ""
    var size : String =""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySourceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appbar)
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
        val location = intent.getStringExtra(location).toString()

        urlSource = url.toString()

        locations = location

        binding.appbar.title = location
        binding.appbar.subtitle = url

        val source = typeSource.getSourceType(location)

        viewmodel.getSource(source,url.toString(),30)

//        val layoutmanager = binding.listSource as GridLayoutManager
//        val totalItems = layoutmanager.itemCount

        binding.appbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.webview -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    true
                }
                R.id.search -> {

                    true
                }

                R.id.info -> {
                    val bottomsheet =BottomsheetInfo.newInstance(url.toString(),size)
                    bottomsheet.show(supportFragmentManager,"info")
                    true
                }
                else -> false}

        }

        binding.again.setOnClickListener {
            viewmodel.getSource(source,url.toString(),30)
        }


        viewmodel.source.observe(this) { source ->
            size = source.size.toString()
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

        viewmodel.search.observe(this){ search ->
            adapterManhwa.updateData(search)
        }

        binding.appbar.setNavigationOnClickListener {
            finish()
        }

        viewmodel.error.observe(this) { error ->
            if (error != null) {
                binding.error.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.menu_navigation_appbar, menu)

        val searchItem = menu?.findItem(R.id.search)
        val searchView = searchItem?.actionView as androidx.appcompat.widget.SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextChange(query: String?): Boolean {
                return false
            }
            override fun onQueryTextSubmit(query: String?): Boolean {

//                Toast.makeText(this@sourceActivity, urlSource, Toast.LENGTH_SHORT).show()

                val source = typeSource.getSourceType(locations)
                viewmodel.search(source,query.toString(),urlSource)

                return true
            }
        })



        return true
    }




    companion object {
        const val TAG = "sourceActivity"
        const val URL = "url"
        const val location = "location"
    }
}