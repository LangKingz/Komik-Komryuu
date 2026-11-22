package com.komik.komik

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.komik.komik.databinding.ActivityMainBinding
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.dao.LibraryDao
import com.komik.komik.service.utils.AppConfigHelper
import com.komik.komik.service.utils.AppUpdateChecker
import com.komik.komik.service.utils.PreferenceManager
import com.komik.komik.service.utils.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: DatabaseRiwayats
    private lateinit var dao: LibraryDao
    private lateinit var navController: NavController
    private var isBottomNavVisible: Boolean = true
    private var maintenanceDialog: Dialog? = null

    // permission launcher
    private val registerLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Snackbar.make(binding.root, "Izin notifikasi ditolak", Snackbar.LENGTH_LONG)
                .setAction("Coba Lagi") { checkPermission() }
                .show()
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            registerLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkPermission()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.getInsetsController(window, window.decorView).let { controller ->
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        db = DatabaseRiwayats.getDatabase(this)
        dao = db.libraryDao()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigation?.setupWithNavController(navController)

        // Show nav bar whenever user switches screen
        navController.addOnDestinationChangedListener { _, _, _ ->
            showBottomNav()
        }

        // Library badge
        CoroutineScope(Dispatchers.IO).launch {
            val count = dao.getLibraryCount()
            withContext(Dispatchers.Main) {
                binding.bottomNavigation?.let { bottomNav ->
                    val badge = bottomNav.getOrCreateBadge(R.id.pustakaFragment)
                    badge.isVisible = count > 0
                    badge.number = count
                }
            }
        }

        // Automatic security & banned check in background
        CoroutineScope(Dispatchers.Main).launch {
            PreferenceManager.checkAccountSecurity(this@MainActivity)
        }

        checkServerConfig()
    }

    private fun checkServerConfig() {
        lifecycleScope.launch {
            val config = AppConfigHelper.cachedConfig ?: AppConfigHelper.fetchConfig()
            val pref = PreferenceManager.getInstance(this@MainActivity)
            val userRole = pref.userRole.lowercase().trim()
            val isPrivileged = userRole == "admin" || userRole == "moderator"

            if (config != null && config.isMaintenance && !isPrivileged) {
                showMaintenanceDialog(config.activeMaintenanceMessage)
                return@launch
            } else {
                maintenanceDialog?.dismiss()
                maintenanceDialog = null
            }

            AppUpdateChecker.checkForUpdate(this@MainActivity, isManual = false)
        }
    }

    private fun showMaintenanceDialog(message: String) {
        if (isFinishing || isDestroyed) return
        if (maintenanceDialog?.isShowing == true) return

        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val view = layoutInflater.inflate(R.layout.dialog_maintenance, null)
        dialog.setContentView(view)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        val tvMessage = view.findViewById<TextView>(R.id.tvMaintenanceMessage)
        val btnRetry = view.findViewById<MaterialButton>(R.id.btnRetryMaintenance)
        val btnExit = view.findViewById<MaterialButton>(R.id.btnExitApp)

        tvMessage?.text = message.ifEmpty {
            "Server Komryuu.x sedang dalam pemeliharaan sistem. Silakan coba beberapa saat lagi."
        }

        btnRetry?.setOnClickListener {
            dialog.dismiss()
            maintenanceDialog = null
            checkServerConfig()
        }

        btnExit?.setOnClickListener {
            dialog.dismiss()
            finishAffinity()
        }

        dialog.window?.let { window ->
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }

        maintenanceDialog = dialog
        dialog.show()
    }

    fun showBottomNav() {
        binding.bottomNavigation?.let { bottomNav ->
            isBottomNavVisible = true
            bottomNav.visibility = View.VISIBLE
            bottomNav.translationY = 0f
            bottomNav.alpha = 1f
        }
    }

    fun hideBottomNav() {
        // Docked bottom bar remains cleanly visible without blank viewport gaps
    }

    fun costumToast(titleNotif: String, context: Context) {
        Toast.makeText(context, titleNotif, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        maintenanceDialog?.dismiss()
        maintenanceDialog = null
    }

    companion object {
        const val TAG = "MainActivity"
    }
}
