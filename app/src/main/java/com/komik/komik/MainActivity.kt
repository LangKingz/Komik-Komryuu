package com.komik.komik

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.snackbar.Snackbar
import android.app.DownloadManager
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.komik.komik.databinding.ActivityMainBinding
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.dao.LibraryDao
import com.komik.komik.service.utils.AppConfigHelper
import com.komik.komik.service.utils.AppUpdateChecker
import com.komik.komik.service.utils.AppUpdateReceiver
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
    private var updateReceiver: AppUpdateReceiver? = null

    private var currentSelectedDestinationId: Int = R.id.homefragment

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

        setupFloatingBottomNav()
        refreshNavProfileAvatar()
        updateLibraryBadge()

        // Automatic security & banned check in background
        CoroutineScope(Dispatchers.Main).launch {
            PreferenceManager.checkAccountSecurity(this@MainActivity)
        }

        handleUpdateNotificationIntent(intent)
        checkServerConfig()
    }

    // ------------------------------------------------------------------
    // Floating Pill Bottom Navigation Bar Setup & Animation
    // ------------------------------------------------------------------
    private fun setupFloatingBottomNav() {
        // Enforce circle crop on profile avatar shapeable image view
        binding.ivNavProfileAvatar.shapeAppearanceModel = binding.ivNavProfileAvatar.shapeAppearanceModel
            .toBuilder()
            .setAllCornerSizes(RelativeCornerSize(0.5f))
            .build()

        binding.tabNavHome.setOnClickListener { selectTab(R.id.homefragment) }
        binding.tabNavPustaka.setOnClickListener { selectTab(R.id.pustakaFragment) }
        binding.tabNavHistory.setOnClickListener { selectTab(R.id.historyFragment) }
        binding.tabNavProfile.setOnClickListener { selectTab(R.id.settingFragment) }

        // Initial selection without animation
        applyTabSelectionUI(R.id.homefragment, previousId = null, animate = false)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            showBottomNav()
            val validTabIds = setOf(
                R.id.homefragment,
                R.id.pustakaFragment,
                R.id.historyFragment,
                R.id.settingFragment
            )
            if (destination.id in validTabIds && destination.id != currentSelectedDestinationId) {
                val prev = currentSelectedDestinationId
                currentSelectedDestinationId = destination.id
                applyTabSelectionUI(destination.id, previousId = prev, animate = true)
            }
        }
    }

    fun selectTab(destinationId: Int) {
        if (navController.currentDestination?.id == destinationId) {
            try {
                binding.root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (_: Exception) {}
            return
        }

        try {
            binding.root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {}

        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(
                navController.graph.findStartDestination().id,
                inclusive = false,
                saveState = true
            )
            .build()

        try {
            navController.navigate(destinationId, null, navOptions)
        } catch (_: Exception) {
            navController.navigate(destinationId)
        }
    }

    /**
     * Interactive icon bounce animation and previous view restorer
     */
    fun animateNavSelection(selectedView: View, previousView: View?) {
        try {
            selectedView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {}

        // Bounce overshoot scale animation (0.85f -> 1.15f -> 1.0f in 260ms)
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.85f, 1.15f, 1.0f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.85f, 1.15f, 1.0f)
        ObjectAnimator.ofPropertyValuesHolder(selectedView, scaleX, scaleY).apply {
            duration = 260
            interpolator = OvershootInterpolator(2.5f)
            start()
        }

        // Return previous view to normal scale
        previousView?.let { prev ->
            prev.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(200)
                .start()
        }
    }

    private fun applyTabSelectionUI(selectedId: Int, previousId: Int?, animate: Boolean) {
        val accentColor = ThemeManager.getAccentColor(this)
        val pillBgColor = ColorUtils.setAlphaComponent(accentColor, 45) // ~18% alpha
        val inactiveColor = Color.parseColor("#8E8E9E")

        data class TabData(
            val id: Int,
            val iconView: View,
            val textView: TextView,
            val pillView: View,
            val isProfile: Boolean = false
        )

        val tabs = listOf(
            TabData(R.id.homefragment, binding.ivNavHome, binding.tvNavHome, binding.pillHighlightHome),
            TabData(R.id.pustakaFragment, binding.ivNavPustaka, binding.tvNavPustaka, binding.pillHighlightPustaka),
            TabData(R.id.historyFragment, binding.ivNavHistory, binding.tvNavHistory, binding.pillHighlightHistory),
            TabData(R.id.settingFragment, binding.ivNavProfileAvatar, binding.tvNavProfile, binding.pillHighlightProfile, isProfile = true)
        )

        val previousTab = tabs.firstOrNull { it.id == previousId }

        tabs.forEach { tab ->
            val isSelected = tab.id == selectedId

            if (isSelected) {
                tab.pillView.backgroundTintList = ColorStateList.valueOf(pillBgColor)
                if (animate) {
                    tab.pillView.animate()
                        .alpha(1.0f)
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(220)
                        .start()
                    animateNavSelection(tab.iconView, previousTab?.iconView)
                } else {
                    tab.pillView.alpha = 1.0f
                    tab.pillView.scaleX = 1.0f
                    tab.pillView.scaleY = 1.0f
                    tab.iconView.scaleX = 1.0f
                    tab.iconView.scaleY = 1.0f
                }

                if (!tab.isProfile) {
                    (tab.iconView as? android.widget.ImageView)?.setColorFilter(accentColor)
                }
                tab.textView.setTextColor(accentColor)
            } else {
                if (animate) {
                    tab.pillView.animate()
                        .alpha(0.0f)
                        .scaleX(0.85f)
                        .scaleY(0.85f)
                        .setDuration(180)
                        .start()
                } else {
                    tab.pillView.alpha = 0.0f
                    tab.pillView.scaleX = 0.85f
                    tab.pillView.scaleY = 0.85f
                    tab.iconView.scaleX = 1.0f
                    tab.iconView.scaleY = 1.0f
                }

                if (!tab.isProfile) {
                    (tab.iconView as? android.widget.ImageView)?.setColorFilter(inactiveColor)
                }
                tab.textView.setTextColor(inactiveColor)
            }
        }

        updateProfileAvatarStroke(selectedId == R.id.settingFragment)
    }

    private fun updateProfileAvatarStroke(isActive: Boolean) {
        val pref = PreferenceManager.getInstance(this)
        val accentColor = ThemeManager.getAccentColor(this)

        val strokeColor = if (isActive) {
            if (pref.isVipOrAbove()) Color.parseColor("#F59E0B") else accentColor
        } else {
            Color.parseColor("#3A3A4E")
        }

        val density = resources.displayMetrics.density
        val strokeWidth = if (isActive) (2f * density) else (1f * density)

        binding.ivNavProfileAvatar.strokeColor = ColorStateList.valueOf(strokeColor)
        binding.ivNavProfileAvatar.strokeWidth = strokeWidth
    }

    /**
     * Public method to refresh user profile photo in the 4th tab
     */
    fun refreshNavProfileAvatar() {
        if (isFinishing || isDestroyed) return
        val user = FirebaseAuth.getInstance().currentUser
        val pref = PreferenceManager.getInstance(this)
        val photoUrl = user?.photoUrl ?: pref.userPhoto

        if (photoUrl != null) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ppanime)
                .error(R.drawable.ppanime)
                .circleCrop()
                .into(binding.ivNavProfileAvatar)
        } else {
            binding.ivNavProfileAvatar.setImageResource(R.drawable.ppanime)
        }

        updateProfileAvatarStroke(currentSelectedDestinationId == R.id.settingFragment)
    }

    fun updateLibraryBadge() {
        // Badge pustaka dinonaktifkan
    }

    // ------------------------------------------------------------------
    // Server Config & App Maintenance Check
    // ------------------------------------------------------------------
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
        if (!isBottomNavVisible) {
            isBottomNavVisible = true
            binding.floatingBottomBar.visibility = View.VISIBLE
            binding.floatingBottomBar.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(220)
                .start()
        }
    }

    fun hideBottomNav() {
        if (isBottomNavVisible) {
            isBottomNavVisible = false
            binding.floatingBottomBar.animate()
                .translationY((binding.floatingBottomBar.height + 60).toFloat())
                .alpha(0f)
                .setDuration(220)
                .withEndAction { binding.floatingBottomBar.visibility = View.GONE }
                .start()
        }
    }

    fun costumToast(titleNotif: String, context: Context) {
        Toast.makeText(context, titleNotif, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        refreshNavProfileAvatar()
        updateLibraryBadge()
        applyTabSelectionUI(currentSelectedDestinationId, previousId = null, animate = false)
    }

    override fun onStart() {
        super.onStart()
        registerUpdateReceiver()
    }

    override fun onStop() {
        super.onStop()
        unregisterUpdateReceiver()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleUpdateNotificationIntent(intent)
    }

    private fun registerUpdateReceiver() {
        if (updateReceiver == null) {
            val receiver = AppUpdateReceiver()
            updateReceiver = receiver
            val filter = IntentFilter().apply {
                addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED)
            }
            ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
            Log.d("APP_UPDATE", "MainActivity: updateReceiver registered")
        }
    }

    private fun unregisterUpdateReceiver() {
        updateReceiver?.let {
            try {
                unregisterReceiver(it)
                Log.d("APP_UPDATE", "MainActivity: updateReceiver unregistered")
            } catch (_: Exception) {}
            updateReceiver = null
        }
    }

    private fun handleUpdateNotificationIntent(intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("APP_UPDATE", "MainActivity handleUpdateNotificationIntent: action=$action")
        if (action == DownloadManager.ACTION_NOTIFICATION_CLICKED) {
            val downloadIds = intent.getLongArrayExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS)
            AppUpdateChecker.handleNotificationClicked(this, downloadIds)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterUpdateReceiver()
        maintenanceDialog?.dismiss()
        maintenanceDialog = null
    }

    companion object {
        const val TAG = "MainActivity"
    }
}
