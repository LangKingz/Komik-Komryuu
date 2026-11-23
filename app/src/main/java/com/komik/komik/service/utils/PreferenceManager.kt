package com.komik.komik.service.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.gson.Gson
import com.komik.komik.BuildConfig
import com.komik.komik.service.api.model.AuthPermissions
import com.komik.komik.service.api.model.AuthRequest
import com.komik.komik.service.api.model.AuthResponse
import com.komik.komik.service.api.model.DeviceInfo
import com.komik.komik.service.client
import com.komik.komik.ui.pages.login.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PreferenceManager(context: Context) {
    private val pref: SharedPreferences = context.getSharedPreferences(PREF_USER, Context.MODE_PRIVATE)

    companion object {
        const val PREF_USER = "user_pref"
        const val KEY_IS_GUEST = "is_guest"
        const val KEY_USER_ROLE = "user_role"
        const val KEY_USER_UID = "user_uid"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_PHOTO = "user_photo"
        const val KEY_LAST_SYNC = "last_sync_timestamp"
        const val KEY_IS_BANNED = "is_banned"
        const val KEY_BAN_REASON = "ban_reason"
        const val KEY_CAN_ACCESS_ADMIN_PANEL = "can_access_admin_panel"
        const val KEY_CAN_ACCESS_NSFW = "can_access_nsfw"
        const val KEY_BYPASS_RATE_LIMIT = "bypass_rate_limit"
        const val KEY_PINNED_SOURCES = "pinned_source_keys"
        const val KEY_AUTO_UPDATE_ENABLED = "auto_update_enabled"
        const val KEY_UPDATE_INTERVAL_HOURS = "update_interval_hours"
        const val KEY_LAST_UPDATE_CHECK = "last_update_check_timestamp"
        const val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
        const val KEY_AUTO_SYNC_HOUR = "auto_sync_hour"
        const val KEY_AUTO_SYNC_MINUTE = "auto_sync_minute"
        const val KEY_AUTO_SYNC_INTERVAL_HOURS = "auto_sync_interval_hours"
        const val KEY_LAST_CLOUD_SYNC_MILLIS = "last_cloud_sync_millis"
        const val KEY_IGNORED_UPDATE_DATE = "ignored_update_date"
        const val KEY_IGNORED_UPDATE_VERSION = "ignored_update_version"
        const val KEY_SELECTED_THEME = "selected_theme_key"
        const val DEFAULT_THEME_KEY = "midnight"
        const val KEY_PUSTAKA_GRID_SPAN = "pustaka_grid_span"
        const val KEY_SOURCE_VIEW_MODE = "source_view_mode" // "grid" or "list"

        @Volatile
        private var instance: PreferenceManager? = null

        fun getInstance(context: Context): PreferenceManager {
            return instance ?: synchronized(this) {
                instance ?: PreferenceManager(context.applicationContext).also { instance = it }
            }
        }

        /**
         * Checks account status with POST /api/v1/auth on Komryuu.x.
         * If banned (HTTP 403 or isBanned == true), clears session, signs out,
         * and shows a non-cancelable AlertDialog.
         */
        suspend fun checkAccountSecurity(
            activity: Activity,
            onSuccess: ((role: String) -> Unit)? = null
        ) {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            val prefManager = getInstance(activity)

            if (user == null || prefManager.isGuest) {
                return
            }

            // Quick check if locally already flagged as banned
            if (prefManager.isBanned) {
                withContext(Dispatchers.Main) {
                    showBannedDialogAndLogout(activity, prefManager.banReason, user.email)
                }
                return
            }

            val request = AuthRequest(
                uid = user.uid,
                email = user.email.orEmpty(),
                name = user.displayName ?: "Pengguna Komryuu",
                photoUrl = user.photoUrl?.toString(),
                provider = "google",
                deviceInfo = DeviceInfo(
                    platform = "android",
                    appVersion = BuildConfig.VERSION_NAME,
                    deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
                )
            )

            try {
                val response = withContext(Dispatchers.IO) {
                    client.KomryuuxSource.authenticateUser(request)
                }

                withContext(Dispatchers.Main) {
                    if (activity.isFinishing || activity.isDestroyed) return@withContext

                    if (response.isSuccessful) {
                        val body = response.body()
                        val isBanned = body?.user?.isBanned == true || body?.code == "USER_BANNED" || (body != null && !body.success)

                        if (body != null && body.success && !isBanned) {
                            val role = body.user?.role ?: "user"
                            prefManager.saveUserSession(
                                user = user,
                                role = role,
                                isBanned = false,
                                banReason = null,
                                permissions = body.effectivePermissions
                            )
                            onSuccess?.invoke(role)
                        } else {
                            val banReason = body?.banReason ?: body?.user?.banReason ?: body?.message
                            prefManager.isBanned = true
                            prefManager.banReason = banReason
                            showBannedDialogAndLogout(activity, banReason, user.email)
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
                        showBannedDialogAndLogout(activity, banReason, user.email)
                    } else {
                        // Offline or other server code, continue with current local session
                        onSuccess?.invoke(prefManager.userRole)
                    }
                }
            } catch (e: Exception) {
                // Network error, proceed gracefully with existing local session
                withContext(Dispatchers.Main) {
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        onSuccess?.invoke(prefManager.userRole)
                    }
                }
            }
        }

        fun showBannedDialogAndLogout(
            activity: Activity,
            banReason: String?,
            email: String?
        ) {
            val prefManager = getInstance(activity)
            prefManager.clearSession()
            FirebaseAuth.getInstance().signOut()
            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                GoogleSignIn.getClient(activity, gso).signOut()
            } catch (_: Exception) {}

            val reasonText = if (!banReason.isNullOrBlank()) {
                banReason
            } else {
                "Pelanggaran terhadap Ketentuan Layanan Komryuu.x."
            }

            MaterialAlertDialogBuilder(activity)
                .setTitle("Akun Diblokir")
                .setMessage("Akun ($email) telah diblokir dari sistem Komryuu.x.\n\nAlasan Pemblokiran:\n\"$reasonText\"\n\nAnda tidak dapat menggunakan akun ini untuk mengakses komik atau sinkronisasi data.")
                .setPositiveButton("Keluar / Kembali ke Login") { dialog, _ ->
                    dialog.dismiss()
                    val intent = Intent(activity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    activity.startActivity(intent)
                    activity.finish()
                }
                .setCancelable(false)
                .show()
        }
    }

    var isGuest: Boolean
        get() = pref.getBoolean(KEY_IS_GUEST, false)
        set(value) = pref.edit().putBoolean(KEY_IS_GUEST, value).apply()

    var userRole: String
        get() = pref.getString(KEY_USER_ROLE, "user") ?: "user"
        set(value) = pref.edit().putString(KEY_USER_ROLE, value).apply()

    var userUid: String?
        get() = pref.getString(KEY_USER_UID, null)
        set(value) = pref.edit().putString(KEY_USER_UID, value).apply()

    var userEmail: String?
        get() = pref.getString(KEY_USER_EMAIL, null)
        set(value) = pref.edit().putString(KEY_USER_EMAIL, value).apply()

    var userName: String?
        get() = pref.getString(KEY_USER_NAME, null)
        set(value) = pref.edit().putString(KEY_USER_NAME, value).apply()

    var userPhoto: String?
        get() = pref.getString(KEY_USER_PHOTO, null)
        set(value) = pref.edit().putString(KEY_USER_PHOTO, value).apply()

    var lastSync: Long
        get() = pref.getLong(KEY_LAST_SYNC, 0L)
        set(value) = pref.edit().putLong(KEY_LAST_SYNC, value).apply()

    var isBanned: Boolean
        get() = pref.getBoolean(KEY_IS_BANNED, false)
        set(value) = pref.edit().putBoolean(KEY_IS_BANNED, value).apply()

    var banReason: String?
        get() = pref.getString(KEY_BAN_REASON, null)
        set(value) = pref.edit().putString(KEY_BAN_REASON, value).apply()

    var canAccessAdminPanel: Boolean
        get() = pref.getBoolean(
            KEY_CAN_ACCESS_ADMIN_PANEL,
            userRole.equals("admin", ignoreCase = true) || userRole.equals("moderator", ignoreCase = true)
        )
        set(value) = pref.edit().putBoolean(KEY_CAN_ACCESS_ADMIN_PANEL, value).apply()

    var canAccessNsfw: Boolean
        get() = pref.getBoolean(KEY_CAN_ACCESS_NSFW, true)
        set(value) = pref.edit().putBoolean(KEY_CAN_ACCESS_NSFW, value).apply()

    var bypassRateLimit: Boolean
        get() = pref.getBoolean(KEY_BYPASS_RATE_LIMIT, false)
        set(value) = pref.edit().putBoolean(KEY_BYPASS_RATE_LIMIT, value).apply()

    var isAutoUpdateEnabled: Boolean
        get() = pref.getBoolean(KEY_AUTO_UPDATE_ENABLED, true)
        set(value) = pref.edit().putBoolean(KEY_AUTO_UPDATE_ENABLED, value).apply()

    var updateIntervalHours: Long
        get() = pref.getLong(KEY_UPDATE_INTERVAL_HOURS, 12L)
        set(value) = pref.edit().putLong(KEY_UPDATE_INTERVAL_HOURS, value).apply()

    var lastUpdateCheckMillis: Long
        get() = pref.getLong(KEY_LAST_UPDATE_CHECK, 0L)
        set(value) = pref.edit().putLong(KEY_LAST_UPDATE_CHECK, value).apply()

    var selectedThemeKey: String
        get() = pref.getString(KEY_SELECTED_THEME, DEFAULT_THEME_KEY) ?: DEFAULT_THEME_KEY
        set(value) = pref.edit().putString(KEY_SELECTED_THEME, value).apply()

    fun saveSelectedThemeSync(key: String): Boolean {
        return pref.edit().putString(KEY_SELECTED_THEME, key).commit()
    }

    var isAutoSyncEnabled: Boolean
        get() = pref.getBoolean(KEY_AUTO_SYNC_ENABLED, true)
        set(value) = pref.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, value).apply()

    var autoSyncHour: Int
        get() = pref.getInt(KEY_AUTO_SYNC_HOUR, 3)
        set(value) = pref.edit().putInt(KEY_AUTO_SYNC_HOUR, value).apply()

    var autoSyncMinute: Int
        get() = pref.getInt(KEY_AUTO_SYNC_MINUTE, 0)
        set(value) = pref.edit().putInt(KEY_AUTO_SYNC_MINUTE, value).apply()

    var autoSyncIntervalHours: Long
        get() = pref.getLong(KEY_AUTO_SYNC_INTERVAL_HOURS, 24L)
        set(value) = pref.edit().putLong(KEY_AUTO_SYNC_INTERVAL_HOURS, value).apply()

    var lastCloudSyncMillis: Long
        get() = pref.getLong(KEY_LAST_CLOUD_SYNC_MILLIS, lastSync)
        set(value) {
            pref.edit().putLong(KEY_LAST_CLOUD_SYNC_MILLIS, value).apply()
            lastSync = value
        }

    var pustakaGridSpan: Int
        get() = pref.getInt(KEY_PUSTAKA_GRID_SPAN, 3)
        set(value) = pref.edit().putInt(KEY_PUSTAKA_GRID_SPAN, value).apply()

    var sourceViewMode: String
        get() = pref.getString(KEY_SOURCE_VIEW_MODE, "grid") ?: "grid"
        set(value) = pref.edit().putString(KEY_SOURCE_VIEW_MODE, value).apply()

    fun getComicShelfStatus(comicId: Int): String {
        return pref.getString("shelf_status_$comicId", "all") ?: "all"
    }

    fun setComicShelfStatus(comicId: Int, status: String) {
        pref.edit().putString("shelf_status_$comicId", status).apply()
    }

    fun setBatchComicShelfStatus(comicIds: Collection<Int>, status: String) {
        val editor = pref.edit()
        for (id in comicIds) {
            editor.putString("shelf_status_$id", status)
        }
        editor.apply()
    }

    fun isVipOrAbove(): Boolean {
        val role = userRole.lowercase().trim()
        return role == "vip" || role == "moderator" || role == "admin"
    }

    private fun getTodayDateString(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    }

    fun isUpdateIgnoredToday(version: String): Boolean {
        val today = getTodayDateString()
        val savedDate = pref.getString(KEY_IGNORED_UPDATE_DATE, "")
        val savedVer = pref.getString(KEY_IGNORED_UPDATE_VERSION, "")
        return savedDate == today && savedVer == version
    }

    fun setUpdateIgnoredToday(version: String) {
        val today = getTodayDateString()
        pref.edit()
            .putString(KEY_IGNORED_UPDATE_DATE, today)
            .putString(KEY_IGNORED_UPDATE_VERSION, version)
            .apply()
    }

    fun saveUserSession(
        user: FirebaseUser,
        role: String = "user",
        isBanned: Boolean = false,
        banReason: String? = null,
        permissions: AuthPermissions? = null
    ) {
        val isAdminOrMod = role.equals("admin", ignoreCase = true) || role.equals("moderator", ignoreCase = true)
        pref.edit()
            .putBoolean(KEY_IS_GUEST, false)
            .putString(KEY_USER_UID, user.uid)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_NAME, user.displayName)
            .putString(KEY_USER_PHOTO, user.photoUrl?.toString())
            .putString(KEY_USER_ROLE, role)
            .putBoolean(KEY_IS_BANNED, isBanned)
            .putString(KEY_BAN_REASON, banReason)
            .putBoolean(KEY_CAN_ACCESS_ADMIN_PANEL, (permissions?.canAccessAdminPanel ?: false) || isAdminOrMod)
            .putBoolean(KEY_CAN_ACCESS_NSFW, permissions?.canAccessNsfw ?: true)
            .putBoolean(KEY_BYPASS_RATE_LIMIT, permissions?.bypassRateLimit ?: false)
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            .apply()
    }

    fun clearSession() {
        pref.edit().clear().apply()
    }

    fun getPinnedSources(): Set<String> {
        return pref.getStringSet(KEY_PINNED_SOURCES, emptySet()) ?: emptySet()
    }

    fun isSourcePinned(sourceKey: String): Boolean {
        if (sourceKey.isBlank()) return false
        return getPinnedSources().contains(sourceKey)
    }

    fun togglePinSource(sourceKey: String): Boolean {
        if (sourceKey.isBlank()) return false
        val currentSet = HashSet(getPinnedSources())
        val isNowPinned = if (currentSet.contains(sourceKey)) {
            currentSet.remove(sourceKey)
            false
        } else {
            currentSet.add(sourceKey)
            true
        }
        pref.edit().putStringSet(KEY_PINNED_SOURCES, currentSet).apply()
        return isNowPinned
    }
}
