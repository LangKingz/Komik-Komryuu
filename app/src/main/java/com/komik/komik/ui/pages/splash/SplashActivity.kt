package com.komik.komik.ui.pages.splash

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.komik.komik.BuildConfig
import com.komik.komik.MainActivity
import com.komik.komik.R
import com.komik.komik.databinding.ActivitySplashBinding
import com.komik.komik.service.api.model.AuthRequest
import com.komik.komik.service.api.model.AuthResponse
import com.komik.komik.service.api.model.DeviceInfo
import com.komik.komik.service.client
import com.komik.komik.service.utils.AppConfigHelper
import com.komik.komik.service.utils.PreferenceManager
import com.komik.komik.service.utils.ThemeManager
import com.komik.komik.ui.pages.login.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    private sealed class AuthStatus {
        data class Success(val role: String) : AuthStatus()
        data class Banned(val reason: String?) : AuthStatus()
        object OfflineOrCached : AuthStatus()
        object Skipped : AuthStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        window.navigationBarColor = ContextCompat.getColor(this, R.color.background_utama)

        WindowCompat.getInsetsController(window, window.decorView).let { controller ->
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }

        setupVisuals()
        startSplashFlow()
    }

    private fun setupVisuals() {
        // Smooth entrance animation for center logo & text
        try {
            val enterAnim = AnimationUtils.loadAnimation(this, R.anim.splash_enter)
            binding.layoutLogoCenter.startAnimation(enterAnim)
        } catch (_: Exception) {}

        // App version string
        binding.tvVersion.text = "Versi ${BuildConfig.VERSION_NAME}"
    }

    private fun startSplashFlow() {
        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()
            val prefManager = PreferenceManager.getInstance(this@SplashActivity)
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser

            if (currentUser != null && !prefManager.isGuest) {
                binding.tvStatus.text = "Memeriksa sesi akun..."
            } else {
                binding.tvStatus.text = "Menghubungkan ke server Komryuu.x..."
            }

            // Parallel checking bounded by a 3.5-second timeout
            val (config, authStatus) = withTimeoutOrNull(3500L) {
                val configDeferred = async(Dispatchers.IO) {
                    try {
                        AppConfigHelper.fetchConfig()
                    } catch (_: Exception) {
                        null
                    }
                }

                val authDeferred = async(Dispatchers.IO) {
                    if (currentUser != null && !prefManager.isGuest) {
                        verifyUserSessionSilently(currentUser, prefManager)
                    } else {
                        AuthStatus.Skipped
                    }
                }

                Pair(configDeferred.await(), authDeferred.await())
            } ?: Pair(AppConfigHelper.cachedConfig, AuthStatus.OfflineOrCached)

            // Ensure smooth splash animation doesn't end abruptly (min 900ms)
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 900) {
                delay(900 - elapsed)
            }

            if (isFinishing || isDestroyed) return@launch

            val userRole = prefManager.userRole

            // 1. Maintenance Check (User with 'admin' role can bypass for testing)
            if (config != null && config.isMaintenance && userRole != "admin") {
                AppConfigHelper.showMaintenanceDialog(
                    activity = this@SplashActivity,
                    message = config.activeMaintenanceMessage,
                    onRetry = { startSplashFlow() }
                )
                return@launch
            }

            // 2. Force / App Update Check
            val update = config?.activeUpdate
            if (update != null) {
                val currentVer = BuildConfig.VERSION_NAME
                val isMinOlder = AppConfigHelper.isVersionOlder(currentVer, update.minAppVersion)
                val isLatestOlder = AppConfigHelper.isVersionOlder(currentVer, update.latestAppVersion)

                if (isMinOlder || (update.forceUpdate && isLatestOlder)) {
                    AppConfigHelper.showUpdateDialog(
                        activity = this@SplashActivity,
                        update = update,
                        isForce = true
                    )
                    return@launch
                } else if (isLatestOlder) {
                    AppConfigHelper.showUpdateDialog(
                        activity = this@SplashActivity,
                        update = update,
                        isForce = false,
                        onLater = { routeNext(currentUser, prefManager, authStatus) }
                    )
                    return@launch
                }
            }

            // 3. Routing
            routeNext(currentUser, prefManager, authStatus)
        }
    }

    private suspend fun verifyUserSessionSilently(
        currentUser: com.google.firebase.auth.FirebaseUser,
        prefManager: PreferenceManager
    ): AuthStatus {
        if (prefManager.isBanned) {
            return AuthStatus.Banned(prefManager.banReason)
        }

        return try {
            val request = AuthRequest(
                uid = currentUser.uid,
                email = currentUser.email.orEmpty(),
                name = currentUser.displayName ?: "Pengguna Komryuu",
                photoUrl = currentUser.photoUrl?.toString(),
                provider = "google",
                deviceInfo = DeviceInfo(
                    platform = "android",
                    appVersion = BuildConfig.VERSION_NAME,
                    deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
                )
            )

            val response = client.KomryuuxSource.authenticateUser(request)
            if (response.isSuccessful) {
                val body = response.body()
                val isBanned = body?.user?.isBanned == true || body?.code == "USER_BANNED" || (body != null && !body.success)

                if (body != null && body.success && !isBanned) {
                    val role = body.user?.role ?: "user"
                    prefManager.saveUserSession(
                        user = currentUser,
                        role = role,
                        isBanned = false,
                        banReason = null,
                        permissions = body.effectivePermissions
                    )
                    AuthStatus.Success(role)
                } else {
                    val banReason = body?.banReason ?: body?.user?.banReason ?: body?.message
                    prefManager.isBanned = true
                    prefManager.banReason = banReason
                    AuthStatus.Banned(banReason)
                }
            } else if (response.code() == 403) {
                val errorString = response.errorBody()?.string()
                val errorBody = try {
                    Gson().fromJson(errorString, AuthResponse::class.java)
                } catch (_: Exception) {
                    null
                }
                val banReason = errorBody?.banReason ?: errorBody?.user?.banReason ?: errorBody?.message
                prefManager.isBanned = true
                prefManager.banReason = banReason
                AuthStatus.Banned(banReason)
            } else {
                AuthStatus.OfflineOrCached
            }
        } catch (_: Exception) {
            AuthStatus.OfflineOrCached
        }
    }

    private fun routeNext(
        currentUser: com.google.firebase.auth.FirebaseUser?,
        prefManager: PreferenceManager,
        authStatus: AuthStatus
    ) {
        if (isFinishing || isDestroyed) return

        // 1. Account banned handling
        if (authStatus is AuthStatus.Banned) {
            PreferenceManager.showBannedDialogAndLogout(this, authStatus.reason, currentUser?.email)
            return
        }

        if (currentUser != null && !prefManager.isGuest) {
            if (prefManager.isBanned) {
                PreferenceManager.showBannedDialogAndLogout(this, prefManager.banReason, currentUser.email)
                return
            }
            navigateToMain()
        } else if (prefManager.isGuest) {
            navigateToMain()
        } else {
            navigateToLogin()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
