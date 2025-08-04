package com.komik.komik

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.komik.komik.databinding.ActivityMainBinding
import com.komik.komik.service.client
import com.komik.komik.service.parser.comic.SourceParser
import com.komik.komik.service.parser.model.getImages
import com.komik.komik.service.parser.model.getListUpdate
import com.komik.komik.service.utils.generateFakeCookie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.Jsoup

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.navigationBarColor = resources.getColor(R.color.bottom_navbar)

        WindowCompat.getInsetsController(window, window.decorView).let { controller ->
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val nav = binding.bottomNavigation
        val navRail = binding.navigationRail
        val navController = findNavController(R.id.nav_host_fragment)

        nav?.setupWithNavController(navController)
        navRail?.setupWithNavController(navController)

        binding.btnSearch?.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Cari Komik")
                .setMessage("Cari komik yang kamu inginkan")
                .setNeutralButton("cancel"){dialog,which ->
                    dialog.dismiss()
                }
                .show()

        }


    }



    companion object {
        const val TAG = "MainActivity"
    }


}