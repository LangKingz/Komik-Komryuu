package com.komik.komik

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.komik.komik.databinding.ActivityMainBinding
import com.komik.komik.service.client
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.dao.LibraryDao
import com.komik.komik.service.parser.comic.SourceParser
import com.komik.komik.service.parser.model.getImages
import com.komik.komik.service.parser.model.getListUpdate
import com.komik.komik.service.utils.generateFakeCookie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var db: DatabaseRiwayats
    private lateinit var dao : LibraryDao

    // permission launcher
    private val registerlauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {

        } else {
            Snackbar.make(binding.root, "Permission Denied", Snackbar.LENGTH_LONG)
                .setAction("request", {
                    checkPermission()
                })
                .show()
        }
    }

    private fun checkPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {

            }

            else -> {
                registerlauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkPermission()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        window.navigationBarColor = resources.getColor(R.color.bottom_navbar)

        WindowCompat.getInsetsController(window, window.decorView).let { controller ->
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val db = DatabaseRiwayats.getDatabase(this)
        val dao = db.libraryDao()

        val nav = binding.bottomNavigation
        val navRail = binding.navigationRail
        val navController = findNavController(R.id.nav_host_fragment)

        ViewCompat.setOnApplyWindowInsetsListener(nav!!) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Ambil pengaturan margin bawaan dari XML
            val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
            // Tambahin margin bawah bawaan (16dp) dengan tinggi navigation bar hp
            layoutParams.bottomMargin = systemBars.bottom + (16 * resources.displayMetrics.density).toInt()
            view.layoutParams = layoutParams

            insets
        }

        CoroutineScope(Dispatchers.IO).launch {
            val count = dao.getLibraryCount()

            withContext(Dispatchers.Main){
                val badge = nav?.getOrCreateBadge(R.id.pustakaFragment)
                badge?.isVisible = true
                badge?.number = count
            }
        }



        nav?.setupWithNavController(navController)
        navRail?.setupWithNavController(navController)




//        binding.btnSearch?.setOnClickListener {
//            MaterialAlertDialogBuilder(this)
//                .setTitle("Cari Komik")
//                .setMessage("Cari komik yang kamu inginkan")
//                .setNeutralButton("cancel"){dialog,which ->
//                    dialog.dismiss()
//                }
//                .show()
//
//        }


    }

    fun costumToast (titleNotif: String, context: Context){
        Toast.makeText(context, titleNotif, Toast.LENGTH_SHORT).show()
    }



    companion object {
        const val TAG = "MainActivity"
    }


}