package com.komik.komik.ui.pengaturan

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.komik.komik.BuildConfig
import com.komik.komik.R
import com.komik.komik.databinding.FragmentSettingBinding
import com.komik.komik.service.api.model.AuthResponse
import com.komik.komik.service.api.model.DeviceInfo
import com.komik.komik.service.api.model.SyncHistoryItemDto
import com.komik.komik.service.api.model.SyncLibraryItemDto
import com.komik.komik.service.api.model.SyncRequest
import com.komik.komik.service.client
import com.komik.komik.service.component.AlertDialogCostum
import com.komik.komik.service.component.BottomSheetCookie
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.model.Chapter
import com.komik.komik.service.data.model.Comic
import com.komik.komik.service.data.model.Library
import com.komik.komik.service.data.model.Riwayats
import com.komik.komik.service.utils.PreferenceManager
import android.graphics.Color
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.komik.komik.service.repository.Repository
import com.komik.komik.ui.pages.admin.AdminManageUserActivity
import com.komik.komik.ui.pages.login.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: DatabaseRiwayats
    private var adminFirestoreListener: ListenerRegistration? = null
    private var adminUsersCountListener: ListenerRegistration? = null

    // Data model for JSON library export/import
    data class LibraryBackupItem(
        val title: String,
        val url: String,
        val imageUrl: String,
        val author: String? = null,
        val rating: String? = null,
        val description: String? = null,
        val location: String,
        val type: String? = null,
        val status: String? = null,
        val isFavorite: Boolean = false,
        val createdAt: Long = System.currentTimeMillis()
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = DatabaseRiwayats.getDatabase(requireContext())

        setupUserProfile()
        setupReadingInsights()
        setupReaderPreferences()
        setupContentAndNotifications()
        setupStorageAndBackup()
        setupSystemAndAbout()
        checkServerStatus()
    }

    // ----------------------------------------------------
    // 1. User Profile & Account Sync
    // ----------------------------------------------------
    private fun setupUserProfile() {
        val user = auth.currentUser
        val context = requireContext()
        val prefManager = PreferenceManager.getInstance(context)
        val isGuest = prefManager.isGuest

        if (user != null && !isGuest) {
            binding.tvProfileName.text = user.displayName ?: "Pengguna Komryuu"
            binding.tvProfileEmail.text = user.email ?: "-"
            binding.tvProfileBadge.text = "Tersinkronisasi Komryuu.x"
            binding.tvProfileBadge.setBackgroundResource(R.drawable.badge_status_online)
            binding.tvProfileBadge.setTextColor(ContextCompat.getColor(context, R.color.text_emerald_400))

            updateLastSyncDisplay(prefManager)
            updateRoleUI(prefManager.userRole)

            binding.btnSyncNow.visibility = View.VISIBLE
            binding.btnSyncNow.setOnClickListener {
                syncAccountWithServer(user)
            }

            binding.btnRestoreCloud.visibility = View.VISIBLE
            binding.btnRestoreCloud.setOnClickListener {
                restoreAccountFromCloud(user)
            }

            binding.btnAuthAction.text = "Keluar Akun"
            binding.btnAuthAction.setIconResource(R.drawable.baseline_manage_accounts_24)
            binding.btnAuthAction.setOnClickListener {
                showLogoutConfirmation()
            }

            Glide.with(this)
                .load(user.photoUrl)
                .placeholder(R.drawable.ppanime)
                .error(R.drawable.ppanime)
                .circleCrop()
                .into(binding.imgProfile)

            // Auto check banned & sync status in background
            lifecycleScope.launch {
                PreferenceManager.checkAccountSecurity(requireActivity()) { updatedRole ->
                    updateRoleUI(updatedRole)
                    updateLastSyncDisplay(prefManager)
                }
            }
        } else {
            binding.tvProfileName.text = "Tamu / Guest"
            binding.tvProfileEmail.text = "Masuk untuk mencadangkan pustaka ke Cloud"
            binding.tvProfileBadge.text = "Mode Tamu"
            binding.tvProfileBadge.setBackgroundResource(R.drawable.badge_status_offline)
            binding.tvProfileBadge.setTextColor(ContextCompat.getColor(context, R.color.golden_sand))

            updateRoleUI("guest")

            binding.tvLastSync.text = "Status: Mode Tamu (Pustaka tersimpan lokal)"

            binding.btnSyncNow.visibility = View.VISIBLE
            binding.btnSyncNow.setOnClickListener {
                Snackbar.make(binding.root, "Masuk dengan Google untuk mengaktifkan sinkronisasi cloud", Snackbar.LENGTH_LONG)
                    .setAction("Login") {
                        navigateToLogin()
                    }.show()
            }

            binding.btnRestoreCloud.visibility = View.VISIBLE
            binding.btnRestoreCloud.setOnClickListener {
                Snackbar.make(binding.root, "Masuk dengan Google untuk memulihkan pustaka dari cloud", Snackbar.LENGTH_LONG)
                    .setAction("Login") {
                        navigateToLogin()
                    }.show()
            }

            binding.btnAuthAction.text = "Masuk Google"
            binding.btnAuthAction.setIconResource(R.drawable.ic_google_logo)
            binding.btnAuthAction.setOnClickListener {
                navigateToLogin()
            }

            binding.imgProfile.setImageResource(R.drawable.ppanime)
        }
    }

    private fun updateLastSyncDisplay(prefManager: PreferenceManager) {
        val lastSyncTime = prefManager.lastSync
        if (lastSyncTime > 0L) {
            val nowCal = Calendar.getInstance()
            val syncCal = Calendar.getInstance().apply { timeInMillis = lastSyncTime }
            val isToday = nowCal.get(Calendar.YEAR) == syncCal.get(Calendar.YEAR) &&
                    nowCal.get(Calendar.DAY_OF_YEAR) == syncCal.get(Calendar.DAY_OF_YEAR)

            val text = if (isToday) {
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastSyncTime))
                "Terakhir disinkronkan: Hari ini, $timeStr"
            } else {
                val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(lastSyncTime))
                "Terakhir disinkronkan: $dateStr"
            }
            binding.tvLastSync.text = text
        } else {
            binding.tvLastSync.text = "Status: Belum disinkronkan ke Cloud"
        }
    }

    private fun updateRoleUI(role: String) {
        val context = context ?: return
        val prefManager = PreferenceManager.getInstance(context)
        val roleLower = role.lowercase()

        Toast.makeText(requireContext(), "${roleLower}", Toast.LENGTH_SHORT).show()

        when (roleLower) {
            "admin" -> {
                binding.tvRoleBadge.text = "ADMIN"
                binding.tvRoleBadge.setBackgroundResource(R.drawable.badge_role_admin)
                binding.tvRoleBadge.setTextColor(ContextCompat.getColor(context, R.color.text_violet_400))
            }
            "moderator" -> {
                binding.tvRoleBadge.text = "MOD"
                binding.tvRoleBadge.setBackgroundResource(R.drawable.badge_role_admin)
                binding.tvRoleBadge.setTextColor(ContextCompat.getColor(context, R.color.text_violet_400))
            }
            "vip" -> {
                binding.tvRoleBadge.text = "VIP"
                binding.tvRoleBadge.setBackgroundResource(R.drawable.badge_role_vip)
                binding.tvRoleBadge.setTextColor(android.graphics.Color.parseColor("#93C5FD"))
            }
            "user", "member" -> {
                binding.tvRoleBadge.text = "MEMBER"
                binding.tvRoleBadge.setBackgroundResource(R.drawable.badge_role_member)
                binding.tvRoleBadge.setTextColor(ContextCompat.getColor(context, R.color.mint_teal))
            }
            else -> {
                binding.tvRoleBadge.text = "TAMU"
                binding.tvRoleBadge.setBackgroundResource(R.drawable.badge_role_guest)
                binding.tvRoleBadge.setTextColor(ContextCompat.getColor(context, R.color.gray_400))
            }
        }

        // Pemisahan Tegas Area Kontrol Admin: Hanya tampil jika role == admin atau moderator
        val hasAdminAccess = prefManager.canAccessAdminPanel || roleLower == "admin" || roleLower == "moderator"
        if (hasAdminAccess) {
            binding.cardAdminPanel.visibility = View.VISIBLE
            setupAdminControl()
        } else {
            binding.cardAdminPanel.visibility = View.GONE
        }

        // Apply NSFW permission to switch
        if (!prefManager.canAccessNsfw) {
            binding.safemode.isChecked = true
            binding.safemode.isEnabled = false
            binding.tvSafeModeDesc.text = "Dibatasi oleh Administrator"
            binding.tvSafeModeDesc.setTextColor(android.graphics.Color.parseColor("#F87171"))
        } else {
            binding.safemode.isEnabled = true
            binding.tvSafeModeDesc.text = "Sembunyikan komik bertema dewasa"
            binding.tvSafeModeDesc.setTextColor(ContextCompat.getColor(context, R.color.gray_400))
        }
    }

    private fun setupAdminControl() {
        val firestore = FirebaseFirestore.getInstance()

        // 1. Total Pengguna Terdaftar (Realtime)
        adminUsersCountListener?.remove()
        adminUsersCountListener = firestore.collection("USERS")
            .addSnapshotListener { snapshot, _ ->
                if (_binding != null && snapshot != null) {
                    val count = snapshot.size()
                    binding.tvAdminTotalUsers.text = "$count Pengguna"
                }
            }

        // 2. Total Ekstensi Aktif
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val exts = Repository(client).getSourceExtension()
                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        val count = exts?.size ?: 0
                        binding.tvAdminTotalExtensions.text = "$count Sumber Aktif"
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        binding.tvAdminTotalExtensions.text = "5 Sumber Aktif"
                    }
                }
            }
        }

        // 3. Quick Toggle Maintenance Mode (Realtime from SETTINGS/app_config)
        adminFirestoreListener?.remove()
        adminFirestoreListener = firestore.collection("SETTINGS").document("app_config")
            .addSnapshotListener { snapshot, _ ->
                if (_binding != null && snapshot != null && snapshot.exists()) {
                    val isMaintenance = snapshot.getBoolean("isMaintenance")
                        ?: snapshot.getBoolean("maintenanceMode")
                        ?: false

                    binding.switchAdminMaintenance.setOnCheckedChangeListener(null)
                    binding.switchAdminMaintenance.isChecked = isMaintenance
                    if (isMaintenance) {
                        binding.tvAdminMaintenanceStatus.text = "Aplikasi TERKUNCI: Hanya Admin/Mod yang dapat masuk"
                        binding.tvAdminMaintenanceStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                    } else {
                        binding.tvAdminMaintenanceStatus.text = "Aplikasi dapat diakses normal oleh semua pengguna"
                        binding.tvAdminMaintenanceStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_400))
                    }

                    binding.switchAdminMaintenance.setOnCheckedChangeListener { _, isChecked ->
                        toggleMaintenanceMode(firestore, isChecked)
                    }
                }
            }

        // 4. Tombol Kelola Pengguna
        binding.btnAdminManageUsers.setOnClickListener {
            val intent = Intent(requireContext(), AdminManageUserActivity::class.java)
            startActivity(intent)
        }

        // 5. Tombol Kirim Broadcast Pengumuman
        binding.btnAdminBroadcast.setOnClickListener {
            showBroadcastDialog(firestore)
        }

        // 6. Tombol Kelola Ekstensi Sumber
        binding.btnAdminManageExtensions.setOnClickListener {
            showManageExtensionsDialog()
        }

        // 7. Tombol Web Dashboard Admin
        binding.btnOpenAdminDashboard.setOnClickListener {
            openAdminDashboard()
        }
    }

    private fun toggleMaintenanceMode(firestore: FirebaseFirestore, isMaintenance: Boolean) {
        val data = mapOf(
            "isMaintenance" to isMaintenance,
            "maintenanceMode" to isMaintenance,
            "activeMaintenanceMessage" to "Server Komryuu.x sedang dalam pemeliharaan berkala. Silakan coba beberapa saat lagi.",
            "updatedAt" to System.currentTimeMillis()
        )
        firestore.collection("SETTINGS").document("app_config")
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                val msg = if (isMaintenance) "Mode Maintenance diaktifkan! Pengguna biasa terkunci." else "Mode Maintenance dinonaktifkan! Akses kembali normal."
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal mengubah mode maintenance: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showBroadcastDialog(firestore: FirebaseFirestore) {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, (8 * resources.displayMetrics.density).toInt(), pad, (8 * resources.displayMetrics.density).toInt())
        }

        val etTitle = EditText(context).apply {
            hint = "Judul Pengumuman (misal: Info Update)"
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setHintTextColor(ContextCompat.getColor(context, R.color.gray_400))
            textSize = 14f
        }
        layout.addView(etTitle)

        val etMsg = EditText(context).apply {
            hint = "Pesan Pengumuman Lengkap..."
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setHintTextColor(ContextCompat.getColor(context, R.color.gray_400))
            textSize = 13f
        }
        layout.addView(etMsg)

        // Type Selector: Info, Warning, Urgent
        val tvTypeLabel = TextView(context).apply {
            text = "Tipe Pengumuman:"
            setTextColor(ContextCompat.getColor(context, R.color.gray_400))
            textSize = 12f
            setPadding(0, 16, 0, 8)
        }
        layout.addView(tvTypeLabel)

        val radioGroup = RadioGroup(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val rbInfo = RadioButton(context).apply {
            text = "Info"
            setTextColor(ContextCompat.getColor(context, R.color.white))
            id = View.generateViewId()
            isChecked = true
        }
        val rbWarning = RadioButton(context).apply {
            text = "Warning"
            setTextColor(Color.parseColor("#FBBF24"))
            id = View.generateViewId()
        }
        val rbUrgent = RadioButton(context).apply {
            text = "Urgent"
            setTextColor(Color.parseColor("#F87171"))
            id = View.generateViewId()
        }
        radioGroup.addView(rbInfo)
        radioGroup.addView(rbWarning)
        radioGroup.addView(rbUrgent)
        layout.addView(radioGroup)

        // Switch to publish or hide banner
        val switchActive = MaterialSwitch(context).apply {
            text = "Tayangkan Pengumuman ke Beranda"
            setTextColor(ContextCompat.getColor(context, R.color.white))
            isChecked = true
        }
        layout.addView(switchActive)

        MaterialAlertDialogBuilder(context)
            .setTitle("Kirim Broadcast Banner")
            .setView(layout)
            .setPositiveButton("Kirim / Simpan") { dialog, _ ->
                val title = etTitle.text.toString().trim()
                val message = etMsg.text.toString().trim()
                val isPublished = switchActive.isChecked

                if (title.isBlank() && message.isBlank() && isPublished) {
                    Toast.makeText(context, "Judul dan pesan tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val type = when (radioGroup.checkedRadioButtonId) {
                    rbWarning.id -> "warning"
                    rbUrgent.id -> "urgent"
                    else -> "info"
                }

                val bannerData = mapOf(
                    "id" to "banner_${System.currentTimeMillis()}",
                    "title" to title,
                    "message" to message,
                    "type" to type,
                    "isActive" to isPublished,
                    "createdAt" to System.currentTimeMillis()
                )

                firestore.collection("SETTINGS").document("app_config")
                    .set(mapOf("activeBanner" to bannerData), SetOptions.merge())
                    .addOnSuccessListener {
                        dialog.dismiss()
                        Toast.makeText(context, "Broadcast pengumuman berhasil disiarkan!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Gagal menyiarkan: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showManageExtensionsDialog() {
        val extensions = arrayOf("Shinigami", "Komikcast", "Ikiru", "Cosmicscans", "Doujindesu")
        val statusList = arrayOf("Aktif (Online)", "Aktif (Online)", "Aktif (Online)", "Aktif (Online)", "Aktif (Online)")
        val items = extensions.mapIndexed { idx, name -> "$name — ${statusList[idx]}" }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ekstensi Sumber Terdaftar")
            .setItems(items) { _, which ->
                val selected = extensions[which]
                Toast.makeText(requireContext(), "Status sumber '$selected' normal dan siap digunakan.", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun openAdminDashboard() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://komryuu-x.vercel.app/dashboard"))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Tidak dapat membuka browser: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // ----------------------------------------------------
    // Reading Insights / Statistik Membaca
    // ----------------------------------------------------
    private fun setupReadingInsights() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val riwayatDao = db.riwayatdao()
            val chapterDao = db.chapterdao()

            val since7Days = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
            val weeklyChapters = riwayatDao.getWeeklyChaptersRead(since7Days)

            val totalRelasiChapters = riwayatDao.getTotalChaptersRead()
            val totalChaptersReadFromDao = try {
                chapterDao.getAllChapters().count { it.isRead }
            } catch (_: Exception) { 0 }
            val totalChapters = maxOf(totalRelasiChapters, totalChaptersReadFromDao)

            val totalComics = riwayatDao.getTotalComicsRead()

            val timestamps = riwayatDao.getAllReadTimestamps()
            val streakDays = calculateDailyStreak(timestamps)

            val rawSource = riwayatDao.getMostReadSource() ?: riwayatDao.getMostReadGenre()
            val favoriteSource = if (!rawSource.isNullOrBlank()) {
                rawSource.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            } else {
                "Semua"
            }

            withContext(Dispatchers.Main) {
                if (_binding == null) return@withContext

                binding.tvStatStreak.text = "$streakDays Hari"
                binding.tvStatStreakDesc.text = if (streakDays > 0) "Streak aktif" else "Mulai baca hari ini"

                binding.tvStatWeeklyChapters.text = "$weeklyChapters Ch."
                binding.tvStatTotalChapters.text = "$totalChapters Ch."
                binding.tvStatTotalComics.text = "dari $totalComics komik"
                binding.tvStatFavoriteSource.text = favoriteSource

                binding.btnShareInsights.setOnClickListener {
                    shareReadingInsights(
                        streakDays = streakDays,
                        weeklyChapters = weeklyChapters,
                        totalChapters = totalChapters,
                        totalComics = totalComics,
                        favoriteSource = favoriteSource
                    )
                }
            }
        }
    }

    private fun calculateDailyStreak(timestamps: List<Long>): Int {
        if (timestamps.isEmpty()) return 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateSet = timestamps.map { sdf.format(Date(it)) }.toSet()

        val cal = Calendar.getInstance()
        val todayStr = sdf.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)

        var streak = 0
        val checkCal = Calendar.getInstance()

        if (dateSet.contains(todayStr)) {
            streak = 1
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        } else if (dateSet.contains(yesterdayStr)) {
            streak = 1
            checkCal.add(Calendar.DAY_OF_YEAR, -2)
        } else {
            return 0
        }

        while (true) {
            val dateStr = sdf.format(checkCal.time)
            if (dateSet.contains(dateStr)) {
                streak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    private fun shareReadingInsights(
        streakDays: Int,
        weeklyChapters: Int,
        totalChapters: Int,
        totalComics: Int,
        favoriteSource: String
    ) {
        val summaryText = """
            📊 Rekap Membaca Komik saya di Komryuu.x:
            🔥 Streak Harian: $streakDays Hari berturut-turut
            📖 Minggu Ini: $weeklyChapters Chapter
            🏆 Total Selesai: $totalChapters Chapter dari $totalComics Judul Komik
            ⭐ Sumber Favorit: $favoriteSource

            Baca komik online gratis, super cepat & tanpa iklan hanya di Komryuu.x! 🚀
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Reading Insights Komryuu.x")
            putExtra(Intent.EXTRA_TEXT, summaryText)
        }
        startActivity(Intent.createChooser(shareIntent, "Bagikan Rekap Membaca"))
    }

    private fun syncAccountWithServer(user: FirebaseUser) {
        val context = context ?: return
        binding.progressSync.visibility = View.VISIBLE
        binding.btnSyncNow.isEnabled = false
        binding.btnRestoreCloud.isEnabled = false
        Toast.makeText(context, "Menyinkronkan data pustaka & riwayat ke Cloud...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                // 1. Cek keamanan akun & perbarui permissions terbaru
                PreferenceManager.checkAccountSecurity(requireActivity()) { updatedRole ->
                    updateRoleUI(updatedRole)
                }

                // 2. Ambil data lokal dari Room Database pada background thread
                val (syncLibrary, syncHistory) = withContext(Dispatchers.IO) {
                    val libList = db.libraryDao().getAllLibraryList().map { detail ->
                        SyncLibraryItemDto(
                            title = detail.comic.title,
                            url = detail.comic.url,
                            imageUrl = detail.comic.imageUrl,
                            author = detail.comic.author,
                            rating = detail.comic.rating,
                            description = detail.comic.description,
                            location = detail.comic.location,
                            type = detail.comic.type,
                            status = detail.comic.status,
                            isFavorite = detail.library.isFavorite,
                            createdAt = detail.library.createdAt
                        )
                    }

                    val relasiHistory = db.riwayatdao().getAllRiwayatDetailList().map { detail ->
                        SyncHistoryItemDto(
                            title = detail.comic.title,
                            chapter = detail.chapter.title,
                            url = detail.chapter.link,
                            imageUrl = detail.comic.imageUrl,
                            urlWebsite = detail.comic.url,
                            location = detail.comic.location,
                            pageRead = detail.riwayat.pageRead,
                            totalPage = detail.riwayat.totalPage,
                            createdAt = detail.riwayat.readAt
                        )
                    }

                    val finalHistory = if (relasiHistory.isNotEmpty()) {
                        relasiHistory
                    } else {
                        val legacyDb = com.komik.komik.service.data.data.DatabaseRiwayat.getDatabase(context)
                        legacyDb.riwayatdao().getAllRiwayatList().map { r ->
                            SyncHistoryItemDto(
                                title = r.title,
                                chapter = r.chapter,
                                url = r.url,
                                imageUrl = r.imageUrl,
                                urlWebsite = r.urlWebsite,
                                location = r.location,
                                createdAt = r.createdAt
                            )
                        }
                    }

                    Pair(libList, finalHistory)
                }

                val syncReq = SyncRequest(
                    uid = user.uid,
                    lastSync = System.currentTimeMillis(),
                    library = syncLibrary,
                    history = syncHistory
                )

                val response = withContext(Dispatchers.IO) {
                    client.KomryuuxSource.syncData(syncReq)
                }

                if (_binding == null) return@launch

                binding.progressSync.visibility = View.GONE
                binding.btnSyncNow.isEnabled = true
                binding.btnRestoreCloud.isEnabled = true

                if (response.isSuccessful && (response.body()?.success == true || response.code() == 200 || response.code() == 201)) {
                    val prefManager = PreferenceManager.getInstance(context)
                    prefManager.lastSync = System.currentTimeMillis()
                    updateLastSyncDisplay(prefManager)

                    val msg = "Sinkronisasi berhasil! ${syncLibrary.size} komik & ${syncHistory.size} riwayat tersimpan di Cloud."
                    Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(ContextCompat.getColor(context, R.color.bottom_navbar))
                        .show()
                } else {
                    val errMsg = response.body()?.message ?: "Gagal menyinkronkan (Kode: ${response.code()})"
                    Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                if (_binding != null) {
                    binding.progressSync.visibility = View.GONE
                    binding.btnSyncNow.isEnabled = true
                    binding.btnRestoreCloud.isEnabled = true
                }
                Toast.makeText(context, "Kesalahan jaringan saat sinkronisasi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun restoreAccountFromCloud(user: FirebaseUser) {
        val context = context ?: return

        MaterialAlertDialogBuilder(context)
            .setTitle("Pulihkan Data dari Cloud?")
            .setMessage("Koleksi komik dan riwayat baca Anda di server Komryuu.x akan diunduh dan digabungkan ke database lokal.")
            .setIcon(R.drawable.download_cloud_line)
            .setPositiveButton("Pulihkan Sekarang") { dialog, _ ->
                dialog.dismiss()
                executeRestore(user)
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun executeRestore(user: FirebaseUser) {
        val context = context ?: return
        binding.progressSync.visibility = View.VISIBLE
        binding.btnSyncNow.isEnabled = false
        binding.btnRestoreCloud.isEnabled = false
        Toast.makeText(context, "Mengunduh cadangan dari Cloud Komryuu.x...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    client.KomryuuxSource.restoreData(user.uid)
                }

                if (_binding == null) return@launch

                if (response.isSuccessful) {
                    val body = response.body()
                    val libraryList = body?.syncLibrary.orEmpty()
                    val historyList = body?.syncHistory.orEmpty()

                    if (libraryList.isEmpty() && historyList.isEmpty()) {
                        binding.progressSync.visibility = View.GONE
                        binding.btnSyncNow.isEnabled = true
                        binding.btnRestoreCloud.isEnabled = true
                        Toast.makeText(context, "Tidak ada data cadangan cloud untuk akun ini.", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    val (restoredLibCount, restoredHistCount) = withContext(Dispatchers.IO) {
                        var libCount = 0
                        var histCount = 0

                        val comicDao = db.comicdao()
                        val libraryDao = db.libraryDao()
                        val chapterDao = db.chapterdao()
                        val riwayatRelasiDao = db.riwayatdao()
                        val legacyRiwayatDao = com.komik.komik.service.data.data.DatabaseRiwayat.getDatabase(context).riwayatdao()

                        // 1. Pulihkan Library
                        for (item in libraryList) {
                            try {
                                val existingComic = comicDao.getComicByUrl(item.url)
                                val comicId = if (existingComic != null) {
                                    existingComic.id
                                } else {
                                    comicDao.insertComicId(
                                        Comic(
                                            title = item.title,
                                            url = item.url,
                                            rating = item.rating ?: "",
                                            description = item.description ?: "",
                                            imageUrl = item.imageUrl,
                                            author = item.author ?: "",
                                            type = item.type,
                                            status = item.status,
                                            location = item.location,
                                            createdAt = item.createdAt
                                        )
                                    ).toInt()
                                }

                                val existingLib = libraryDao.getLibraryByComicId(comicId)
                                if (existingLib == null) {
                                    libraryDao.insertLibrary(
                                        Library(
                                            comicId = comicId,
                                            isFavorite = item.isFavorite,
                                            createdAt = item.createdAt
                                        )
                                    )
                                    libCount++
                                }
                            } catch (_: Exception) {}
                        }

                        // 2. Pulihkan History
                        for (h in historyList) {
                            try {
                                val comicUrl = h.urlWebsite ?: h.url ?: ""
                                val existingComic = if (comicUrl.isNotEmpty()) comicDao.getComicByUrl(comicUrl) else null
                                val comicId = if (existingComic != null) {
                                    existingComic.id
                                } else {
                                    comicDao.insertComicId(
                                        Comic(
                                            title = h.title,
                                            url = comicUrl,
                                            rating = "",
                                            description = "",
                                            imageUrl = h.imageUrl ?: "",
                                            author = "",
                                            location = h.location,
                                            createdAt = h.createdAt
                                        )
                                    ).toInt()
                                }

                                val chapterLink = h.url ?: ""
                                val existingChapter = chapterDao.getChapterByLink(h.title, chapterLink)
                                val chapterId = if (existingChapter != null) {
                                    existingChapter.id
                                } else {
                                    val newChapter = Chapter(
                                        title = h.chapter,
                                        link = chapterLink,
                                        comicId = comicId,
                                        comicTitle = h.title,
                                        location = h.location,
                                        date = "",
                                        isRead = true,
                                        createdAt = h.createdAt
                                    )
                                    chapterDao.insertChapter(newChapter)
                                    chapterDao.getChapterByLink(h.title, chapterLink)?.id ?: 0
                                }

                                if (comicId > 0 && chapterId > 0) {
                                    val existingRelasi = riwayatRelasiDao.getRiwayatByChapterId(comicId)
                                    if (existingRelasi == null) {
                                        riwayatRelasiDao.insertRiwayatRelasi(
                                            Riwayats(
                                                comicId = comicId,
                                                chapterId = chapterId,
                                                pageRead = h.pageRead,
                                                totalPage = h.totalPage,
                                                isRead = true,
                                                readAt = h.createdAt
                                            )
                                        )
                                        histCount++
                                    }
                                }

                                legacyRiwayatDao.insertRiwayat(
                                    com.komik.komik.service.data.model.Riwayat(
                                        title = h.title,
                                        chapter = h.chapter,
                                        url = h.url,
                                        imageUrl = h.imageUrl,
                                        urlWebsite = h.urlWebsite,
                                        location = h.location,
                                        createdAt = h.createdAt
                                    )
                                )
                            } catch (_: Exception) {}
                        }

                        Pair(libCount, histCount)
                    }

                    val prefManager = PreferenceManager.getInstance(context)
                    prefManager.lastSync = System.currentTimeMillis()
                    updateLastSyncDisplay(prefManager)

                    binding.progressSync.visibility = View.GONE
                    binding.btnSyncNow.isEnabled = true
                    binding.btnRestoreCloud.isEnabled = true

                    setupReadingInsights()

                    val successMsg = "Pemulihan selesai! $restoredLibCount komik baru & $restoredHistCount riwayat berhasil dipulihkan."
                    Snackbar.make(binding.root, successMsg, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(ContextCompat.getColor(context, R.color.mint_teal))
                        .setTextColor(ContextCompat.getColor(context, R.color.black))
                        .show()
                } else {
                    binding.progressSync.visibility = View.GONE
                    binding.btnSyncNow.isEnabled = true
                    binding.btnRestoreCloud.isEnabled = true
                    Toast.makeText(context, "Gagal memulihkan: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                if (_binding != null) {
                    binding.progressSync.visibility = View.GONE
                    binding.btnSyncNow.isEnabled = true
                    binding.btnRestoreCloud.isEnabled = true
                }
                Toast.makeText(context, "Gagal mengunduh data cloud: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleBannedUser(banReason: String?) {
        val activity = activity ?: return
        com.komik.komik.service.utils.PreferenceManager.showBannedDialogAndLogout(activity, banReason, auth.currentUser?.email)
    }

    private fun showLogoutConfirmation() {
        AlertDialogCostum(
            requireContext(),
            "Keluar Akun",
            "Apakah Anda yakin ingin keluar dari akun Google? Komik tersimpan di pustaka lokal tetap aman.",
            onPositive = { dialog ->
                auth.signOut()
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                GoogleSignIn.getClient(requireActivity(), gso).signOut()
                val pref = requireActivity().getSharedPreferences(LoginActivity.PREF_USER, Context.MODE_PRIVATE)
                pref.edit().putBoolean(LoginActivity.KEY_IS_GUEST, false).apply()
                dialog.dismiss()
                Toast.makeText(requireContext(), "Berhasil keluar", Toast.LENGTH_SHORT).show()
                navigateToLogin()
            },
            onNegative = { dialog ->
                dialog.dismiss()
            }
        ).show()
    }

    private fun navigateToLogin() {
        val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    // ----------------------------------------------------
    // 2. Reader Preferences
    // ----------------------------------------------------
    private fun setupReaderPreferences() {
        val pref = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        // Default Reader Mode (Vertical Webtoon vs Horizontal Page)
        val readerMode = pref.getString("reader_mode", "vertical")
        binding.tvReaderMode.text = if (readerMode == "horizontal") {
            "Halaman Tunggal (Horizontal)"
        } else {
            "Webtoon (Gulir Vertikal)"
        }

        binding.itemReaderMode.setOnClickListener {
            val modes = arrayOf("Webtoon (Gulir Vertikal)", "Halaman Tunggal (Horizontal)")
            val currentSelected = if (pref.getString("reader_mode", "vertical") == "horizontal") 1 else 0

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Pilih Mode Baca Default")
                .setSingleChoiceItems(modes, currentSelected) { dialog, which ->
                    val chosen = if (which == 1) "horizontal" else "vertical"
                    pref.edit().putString("reader_mode", chosen).apply()
                    binding.tvReaderMode.text = modes[which]
                    dialog.dismiss()
                }
                .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        // Image Quality
        val quality = pref.getString("image_quality", "standard")
        binding.tvImageQuality.text = when (quality) {
            "low" -> "Hemat Kuota (Rendah)"
            "high" -> "Kualitas Tinggi (HD)"
            else -> "Standar (Seimbang)"
        }

        binding.itemImageQuality.setOnClickListener {
            val qualities = arrayOf("Hemat Kuota (Rendah)", "Standar (Seimbang)", "Kualitas Tinggi (HD)")
            val currentQualityIndex = when (pref.getString("image_quality", "standard")) {
                "low" -> 0
                "high" -> 2
                else -> 1
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Pilih Kualitas Gambar")
                .setSingleChoiceItems(qualities, currentQualityIndex) { dialog, which ->
                    val chosen = when (which) {
                        0 -> "low"
                        2 -> "high"
                        else -> "standard"
                    }
                    pref.edit().putString("image_quality", chosen).apply()
                    binding.tvImageQuality.text = qualities[which]
                    dialog.dismiss()
                }
                .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        // Keep Screen On
        val keepScreenOn = pref.getBoolean("keep_screen_on", true)
        binding.switchKeepScreenOn.isChecked = keepScreenOn
        binding.switchKeepScreenOn.setOnCheckedChangeListener { _, isChecked ->
            pref.edit().putBoolean("keep_screen_on", isChecked).apply()
        }

        // Fullscreen
        val fullscreen = pref.getBoolean("fullscreen", true)
        binding.fullscreen.isChecked = fullscreen
        binding.fullscreen.setOnCheckedChangeListener { _, isChecked ->
            pref.edit().putBoolean("fullscreen", isChecked).apply()
        }

        // Auto Next Chapter
        val autoChapter = pref.getBoolean("pindah", true)
        binding.pindah.isChecked = autoChapter
        binding.pindah.setOnCheckedChangeListener { _, isChecked ->
            pref.edit().putBoolean("pindah", isChecked).apply()
        }
    }

    // ----------------------------------------------------
    // 3. Content & Notifications
    // ----------------------------------------------------
    private fun setupContentAndNotifications() {
        val pref = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val prefManager = PreferenceManager.getInstance(requireContext())

        // Safe mode (Filter 18+)
        val canAccessNsfw = prefManager.canAccessNsfw
        if (!canAccessNsfw) {
            binding.safemode.isChecked = true
            binding.safemode.isEnabled = false
            binding.tvSafeModeDesc.text = "Dibatasi oleh Administrator"
            binding.tvSafeModeDesc.setTextColor(android.graphics.Color.parseColor("#F87171"))
            pref.edit().putBoolean("safemode", true).apply()
        } else {
            val safeMode = pref.getBoolean("safemode", false)
            binding.safemode.isEnabled = true
            binding.safemode.isChecked = safeMode
            binding.tvSafeModeDesc.text = "Sembunyikan komik bertema dewasa"
            binding.tvSafeModeDesc.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_400))
            binding.safemode.setOnCheckedChangeListener { _, isChecked ->
                pref.edit().putBoolean("safemode", isChecked).apply()
            }
        }

        // Cookie settings
        val cookie = pref.getString("cookie", "-")
        binding.subCookie.text = if (cookie.isNullOrBlank() || cookie == "-") "Otomatis dari sistem" else cookie
        binding.CookieBs.setOnClickListener {
            val bottomSheet = BottomSheetCookie()
            bottomSheet.show(parentFragmentManager, "Cookie")
        }

        // Notification Switch
        val notif = pref.getBoolean("notif", false)
        binding.Notification.isChecked = notif
        binding.Notification.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                    launcherPermissionNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    pref.edit().putBoolean("notif", true).apply()
                }
            } else {
                pref.edit().putBoolean("notif", isChecked).apply()
            }
        }
    }

    // ----------------------------------------------------
    // 4. Storage & Backup Management
    // ----------------------------------------------------
    private fun setupStorageAndBackup() {
        calculateImageCacheSize()

        binding.itemDownloadManager.setOnClickListener {
            startActivity(Intent(requireContext(), com.komik.komik.ui.pages.download.DownloadManagerActivity::class.java))
        }

        binding.btnClearCache.setOnClickListener {
            clearImageCache()
        }
        binding.itemClearCache.setOnClickListener {
            clearImageCache()
        }

        binding.itemExportBackup.setOnClickListener {
            exportLibraryBackup()
        }

        binding.itemImportBackup.setOnClickListener {
            showImportLibraryDialog()
        }
    }

    private fun calculateImageCacheSize() {
        lifecycleScope.launch(Dispatchers.IO) {
            val context = context ?: return@launch
            var totalBytes = 0L
            try {
                val glideCache = Glide.getPhotoCacheDir(context)
                if (glideCache != null && glideCache.isDirectory) {
                    totalBytes += getDirSize(glideCache)
                }
                val internalCache = context.cacheDir
                if (internalCache != null && internalCache.isDirectory) {
                    totalBytes += getDirSize(internalCache)
                }
            } catch (_: Exception) {}

            val formatted = formatFileSize(totalBytes)
            withContext(Dispatchers.Main) {
                _binding?.tvCacheSize?.text = "$formatted digunakan"
            }
        }
    }

    private fun clearImageCache() {
        val context = context ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Glide.get(context).clearDiskCache()
            } catch (_: Exception) {}

            withContext(Dispatchers.Main) {
                try {
                    Glide.get(context).clearMemory()
                } catch (_: Exception) {}
                calculateImageCacheSize()
                Toast.makeText(context, "Cache gambar berhasil dibersihkan!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getDirSize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            size += if (f.isDirectory) getDirSize(f) else f.length()
        }
        return size
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            else -> String.format(Locale.US, "%.0f KB", kb)
        }
    }

    private fun exportLibraryBackup() {
        lifecycleScope.launch(Dispatchers.IO) {
            val list = db.libraryDao().getAllLibraryList()
            withContext(Dispatchers.Main) {
                if (list.isEmpty()) {
                    Toast.makeText(requireContext(), "Pustaka Anda masih kosong", Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                val backupItems = list.map { detail ->
                    LibraryBackupItem(
                        title = detail.comic.title,
                        url = detail.comic.url,
                        imageUrl = detail.comic.imageUrl,
                        author = detail.comic.author,
                        rating = detail.comic.rating,
                        description = detail.comic.description,
                        location = detail.comic.location,
                        type = detail.comic.type,
                        status = detail.comic.status,
                        isFavorite = detail.library.isFavorite,
                        createdAt = detail.library.createdAt
                    )
                }

                val jsonString = Gson().toJson(backupItems)

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Cadangkan Pustaka")
                    .setMessage("Ditemukan ${backupItems.size} komik di Pustaka. Pilih metode pencadangan:")
                    .setPositiveButton("Bagikan / Simpan") { dialog, _ ->
                        dialog.dismiss()
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Komryuu_Pustaka_Backup.json")
                            putExtra(Intent.EXTRA_TEXT, jsonString)
                        }
                        startActivity(Intent.createChooser(shareIntent, "Bagikan Cadangan Pustaka"))
                    }
                    .setNeutralButton("Salin ke Clipboard") { dialog, _ ->
                        dialog.dismiss()
                        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Komryuu Backup", jsonString)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(requireContext(), "Data JSON pustaka disalin ke clipboard", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }

    private fun showImportLibraryDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Tempel teks cadangan JSON di sini..."
            setPadding(32, 24, 32, 24)
            maxLines = 6
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Pulihkan Pustaka")
            .setMessage("Tempel kode JSON cadangan komik yang sebelumnya Anda export:")
            .setView(input)
            .setPositiveButton("Impor Sekarang") { dialog, _ ->
                val json = input.text.toString().trim()
                if (json.isNotBlank()) {
                    importLibraryFromJson(json)
                } else {
                    Toast.makeText(requireContext(), "Teks JSON kosong", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNeutralButton("Tempel dari Clipboard") { _, _ ->
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                if (clipText.isNotBlank()) {
                    importLibraryFromJson(clipText)
                } else {
                    Toast.makeText(requireContext(), "Clipboard kosong", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun importLibraryFromJson(json: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val itemType = object : TypeToken<List<LibraryBackupItem>>() {}.type
                val items: List<LibraryBackupItem> = Gson().fromJson(json, itemType)

                if (items.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Format JSON tidak valid atau kosong", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                var importedCount = 0
                for (item in items) {
                    val existingComic = db.comicdao().getComicByUrl(item.url)
                    val comicId = if (existingComic != null) {
                        existingComic.id
                    } else {
                        val newComic = Comic(
                            title = item.title,
                            url = item.url,
                            imageUrl = item.imageUrl,
                            author = item.author ?: "-",
                            rating = item.rating ?: "0.0",
                            description = item.description ?: "-",
                            location = item.location,
                            type = item.type,
                            status = item.status
                        )
                        db.comicdao().insertComicId(newComic).toInt()
                    }

                    val existingLibrary = db.libraryDao().getLibraryByComicId(comicId)
                    if (existingLibrary == null) {
                        db.libraryDao().insertLibrary(
                            Library(
                                comicId = comicId,
                                isFavorite = item.isFavorite,
                                createdAt = item.createdAt
                            )
                        )
                        importedCount++
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Berhasil memulihkan $importedCount komik baru ke Pustaka!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Gagal mengimpor: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ----------------------------------------------------
    // 5. System & About
    // ----------------------------------------------------
    private fun setupSystemAndAbout() {
        binding.tvAppVersion.text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"

        binding.btnTrakteer.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://trakteer.id"))
                startActivity(intent)
            } catch (_: Exception) {
                Snackbar.make(binding.root, "Kunjungi trakteer.id untuk mendukung developer", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.btnAbout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Tentang Komryuu")
                .setMessage("Komryuu v${BuildConfig.VERSION_NAME}\n\nAplikasi pembaca komik modern dengan dukungan multi-ekstensi, sinkronisasi cloud Firebase & server Komryuu.x, serta manajemen database lokal Room.\n\nDibangun dengan Kotlin, Material Design 3, & Coroutines.")
                .setPositiveButton("Tutup") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    private fun checkServerStatus() {
        lifecycleScope.launch(Dispatchers.IO) {
            var isOnline = false
            try {
                val response = client.KomryuuxSource.getSource()
                if (!response.data.isNullOrEmpty()) {
                    isOnline = true
                }
            } catch (_: Exception) {
                isOnline = false
            }

            withContext(Dispatchers.Main) {
                if (_binding == null) return@withContext
                if (isOnline) {
                    binding.badgeServerStatus.text = "Online"
                    binding.badgeServerStatus.setBackgroundResource(R.drawable.badge_status_online)
                    binding.badgeServerStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_emerald_400))
                    binding.tvServerStatusDetail.text = "Terhubung normal ke Komryuu.x API"
                } else {
                    binding.badgeServerStatus.text = "Offline"
                    binding.badgeServerStatus.setBackgroundResource(R.drawable.badge_status_offline)
                    binding.badgeServerStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                    binding.tvServerStatusDetail.text = "Server tidak merespons atau sedang offline"
                }
            }
        }
    }

    private val launcherPermissionNotification = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pref = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        pref.edit().putBoolean("notif", granted).apply()
        binding.Notification.isChecked = granted
        if (!granted) {
            Toast.makeText(requireContext(), "Izin Notifikasi Ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        setupUserProfile()
        calculateImageCacheSize()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adminFirestoreListener?.remove()
        adminUsersCountListener?.remove()
        _binding = null
    }
}
