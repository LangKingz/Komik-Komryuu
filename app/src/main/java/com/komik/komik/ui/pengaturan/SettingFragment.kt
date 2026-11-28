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
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.collectLatest
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
import com.komik.komik.service.api.model.ReportItem
import com.komik.komik.service.api.model.SyncHistoryItem
import com.komik.komik.service.api.model.SyncLibraryItem
import com.komik.komik.service.api.model.SyncRequest
import com.komik.komik.service.client
import com.komik.komik.service.component.AlertDialogCostum
import com.komik.komik.service.component.BottomSheetCookie
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.model.Chapter
import com.komik.komik.service.data.model.Comic
import com.komik.komik.service.data.model.Library
import com.komik.komik.service.adapter.AdapterThemePreset
import com.komik.komik.service.data.model.Riwayats
import com.komik.komik.service.api.model.BroadcastBanner
import com.komik.komik.service.api.model.UpdateConfigRequest
import com.komik.komik.service.utils.AdminHelper
import com.komik.komik.service.utils.AppUpdateChecker
import com.komik.komik.service.utils.PreferenceManager
import com.komik.komik.service.utils.ThemeManager
import com.komik.komik.service.worker.UpdateScheduler
import com.komik.komik.service.worker.AutoSyncScheduler
import android.graphics.Color
import android.util.Log
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
import com.komik.komik.ui.pages.admin.AdminManageExtensiActivity
import com.komik.komik.ui.pages.admin.AdminManageReportsActivity
import com.komik.komik.ui.pages.admin.AdminBroadcastConfigActivity
import com.komik.komik.ui.pages.login.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.komik.komik.service.adapter.AdminUserModel

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
        setupThemeSelector()
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
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: prefManager.userUid
                if (!uid.isNullOrBlank()) {
                    syncAccountWithServer(null)
                } else {
                    Snackbar.make(binding.root, "Masuk dengan Google untuk mengaktifkan sinkronisasi cloud", Snackbar.LENGTH_LONG)
                        .setAction("Login") {
                            navigateToLogin()
                        }.show()
                }
            }

            binding.btnRestoreCloud.visibility = View.VISIBLE
            binding.btnRestoreCloud.setOnClickListener {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: prefManager.userUid
                if (!uid.isNullOrBlank()) {
                    restoreAccountFromCloud(null)
                } else {
                    Snackbar.make(binding.root, "Masuk dengan Google untuk memulihkan pustaka dari cloud", Snackbar.LENGTH_LONG)
                        .setAction("Login") {
                            navigateToLogin()
                        }.show()
                }
            }

            binding.btnAuthAction.text = "Masuk Google"
            binding.btnAuthAction.setIconResource(R.drawable.ic_google_logo)
            binding.btnAuthAction.setOnClickListener {
                navigateToLogin()
            }

            binding.imgProfile.setImageResource(R.drawable.ppanime)
        }

        setupAutoSyncScheduleControls(prefManager, isLogged = user != null && !isGuest)
    }

    private fun setupAutoSyncScheduleControls(prefManager: PreferenceManager, isLogged: Boolean) {
        binding.dividerAutoSync.visibility = if (isLogged) View.VISIBLE else View.GONE
        binding.layoutAutoSyncSchedule.visibility = if (isLogged) View.VISIBLE else View.GONE
        if (!isLogged) return

        fun updateAutoSyncDisplay() {
            val hour = prefManager.autoSyncHour
            val min = prefManager.autoSyncMinute
            val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", hour, min)
            val interval = prefManager.autoSyncIntervalHours

            binding.tvAutoSyncTime.text = if (interval == 12L) {
                "Setiap 12 jam (Mulai pukul $timeFormatted)"
            } else {
                "Setiap hari pukul $timeFormatted"
            }

            binding.tvAutoSyncInterval.text = if (interval == 12L) {
                "Setiap 12 Jam (2x Sehari)"
            } else {
                "Setiap 24 Jam (1x Sehari)"
            }
        }

        binding.switchAutoSync.isChecked = prefManager.isAutoSyncEnabled
        binding.switchAutoSync.setOnCheckedChangeListener { _, isChecked ->
            prefManager.isAutoSyncEnabled = isChecked
            if (isChecked) {
                AutoSyncScheduler.scheduleAutoSync(requireContext())
                Toast.makeText(requireContext(), "Auto Cloud Sync dijadwalkan", Toast.LENGTH_SHORT).show()
            } else {
                AutoSyncScheduler.cancelAutoSync(requireContext())
                Toast.makeText(requireContext(), "Auto Cloud Sync dinonaktifkan", Toast.LENGTH_SHORT).show()
            }
        }

        binding.itemAutoSyncTime.setOnClickListener {
            val currentHour = prefManager.autoSyncHour
            val currentMinute = prefManager.autoSyncMinute
            val timePicker = android.app.TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    prefManager.autoSyncHour = hourOfDay
                    prefManager.autoSyncMinute = minute
                    updateAutoSyncDisplay()
                    if (prefManager.isAutoSyncEnabled) {
                        AutoSyncScheduler.scheduleAutoSync(requireContext())
                    }
                    val timeStr = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                    Toast.makeText(requireContext(), "Jadwal sinkronisasi disetel ke pukul $timeStr", Toast.LENGTH_SHORT).show()
                },
                currentHour,
                currentMinute,
                true
            )
            timePicker.setTitle("Pilih Jam Sinkronisasi")
            timePicker.show()
        }

        binding.itemAutoSyncInterval.setOnClickListener {
            val options = arrayOf(
                "Setiap 24 Jam (1x Sehari pada jam terpilih)",
                "Setiap 12 Jam (2x Sehari)"
            )
            val currentInterval = prefManager.autoSyncIntervalHours
            val selectedIdx = if (currentInterval == 12L) 1 else 0

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Frekuensi Sinkronisasi Cloud")
                .setSingleChoiceItems(options, selectedIdx) { dialog, which ->
                    prefManager.autoSyncIntervalHours = if (which == 1) 12L else 24L
                    updateAutoSyncDisplay()
                    if (prefManager.isAutoSyncEnabled) {
                        AutoSyncScheduler.scheduleAutoSync(requireContext())
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        updateAutoSyncDisplay()
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
                binding.tvRoleBadge.text = "VIP MEMBER"
                binding.tvRoleBadge.setBackgroundResource(R.drawable.badge_role_vip)
                binding.tvRoleBadge.setTextColor(android.graphics.Color.parseColor("#FBBF24"))
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

        // Tampilan Minimalis Elegan VIP Access Card
        val isVip = prefManager.isVipOrAbove()
        if (isVip) {
            binding.cardUserProfile.strokeColor = android.graphics.Color.parseColor("#F59E0B")
            binding.cardUserProfile.strokeWidth = (2 * resources.displayMetrics.density).toInt()
            binding.layoutVipBenefits.visibility = View.VISIBLE
        } else {
            binding.cardUserProfile.strokeColor = android.graphics.Color.parseColor("#3A404D")
            binding.cardUserProfile.strokeWidth = (1 * resources.displayMetrics.density).toInt()
            binding.layoutVipBenefits.visibility = View.GONE
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

        // 4. Tombol 2x2 Grid Aksi Kontrol Admin
        // Card 1: Kelola Pengguna & VIP
        binding.btnAdminManageUsers.setOnClickListener {
            val intent = Intent(requireContext(), AdminManageUserActivity::class.java)
            startActivity(intent)
        }

        // Card 2: Kelola Ekstensi Sumber
        binding.btnAdminManageExtensions.setOnClickListener {
            val intent = Intent(requireContext(), AdminManageExtensiActivity::class.java)
            startActivity(intent)
        }

        // Card 3: Tiket Laporan User
        binding.btnAdminManageReports.setOnClickListener {
            val intent = Intent(requireContext(), AdminManageReportsActivity::class.java)
            startActivity(intent)
        }

        // Card 4: Broadcast & Maintenance Server
        binding.btnAdminBroadcastConfig.setOnClickListener {
            val intent = Intent(requireContext(), AdminBroadcastConfigActivity::class.java)
            startActivity(intent)
        }

        // 5. Muat Badge Pending Reports Count
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = AdminHelper.fetchAdminToken()
                Log.d("ADMIN_SYNC", "GET /api/v1/reports: tokenPresent=${token != null}")
                val reportsRes = client.KomryuuxSource.getReports(authHeader = token)
                Log.d("ADMIN_SYNC", "Response GET /api/v1/reports: status=${reportsRes.code()}, isSuccess=${reportsRes.isSuccessful}")
                if (reportsRes.isSuccessful && reportsRes.body() != null) {
                    val reports = reportsRes.body()?.data.orEmpty()
                    val pendingCount = reports.count { it.status.equals("pending", true) || it.status.equals("menunggu", true) }
                    withContext(Dispatchers.Main) {
                        if (_binding != null) {
                            if (pendingCount > 0) {
                                binding.tvPendingReportsBadge.visibility = View.VISIBLE
                                binding.tvPendingReportsBadge.text = "$pendingCount Baru"
                            } else {
                                binding.tvPendingReportsBadge.visibility = View.GONE
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("ADMIN_SYNC", "Gagal memuat badge pending reports: ${e.message}")
            }
        }

        // 6. Tombol Web Dashboard Admin
        binding.btnOpenAdminDashboard.setOnClickListener {
            openAdminDashboard()
        }
    }

    private fun toggleMaintenanceMode(firestore: FirebaseFirestore, isMaintenance: Boolean) {
        val maintenanceMsg = "Server Komryuu.x sedang dalam pemeliharaan berkala. Silakan coba beberapa saat lagi."
        val data = mapOf(
            "isMaintenance" to isMaintenance,
            "maintenanceMode" to isMaintenance,
            "activeMaintenanceMessage" to maintenanceMsg,
            "updatedAt" to System.currentTimeMillis()
        )
        firestore.collection("SETTINGS").document("app_config")
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                val msg = if (isMaintenance) "Mode Maintenance diaktifkan di Firebase!" else "Mode Maintenance dinonaktifkan di Firebase!"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal mengubah mode maintenance di Firebase: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        // Sinkronisasi ke REST API POST api/v1/config
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = AdminHelper.fetchAdminToken()
                val request = UpdateConfigRequest(
                    maintenanceMode = isMaintenance,
                    maintenanceMessage = maintenanceMsg
                )
                val payloadJson = Gson().toJson(request)
                Log.d("ADMIN_SYNC", "POST /api/v1/config: payload=$payloadJson, tokenPresent=${token != null}")

                var res = client.KomryuuxSource.updateAppConfig(authHeader = token, request = request)
                if (res.code() == 405) {
                    Log.d("ADMIN_SYNC", "POST /api/v1/config returned 405, fallback ke PUT /api/v1/config")
                    res = client.KomryuuxSource.putAppConfig(authHeader = token, request = request)
                }
                Log.d("ADMIN_SYNC", "Response /api/v1/config: status=${res.code()}, isSuccess=${res.isSuccessful}, msg=${res.body()?.message}")

                withContext(Dispatchers.Main) {
                    val context = context ?: return@withContext
                    if (res.isSuccessful) {
                        val serverMsg = res.body()?.message ?: (if (isMaintenance) "Mode Maintenance server aktif!" else "Mode Maintenance server dinonaktifkan!")
                        Toast.makeText(context, serverMsg, Toast.LENGTH_SHORT).show()
                    } else {
                        val errBody = res.errorBody()?.string()
                        Log.e("ADMIN_SYNC", "toggleMaintenanceMode gagal HTTP ${res.code()}: $errBody")
                        Toast.makeText(context, "Sinkronisasi server gagal (${res.code()})", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ADMIN_SYNC", "Exception toggleMaintenanceMode: ${e.message}", e)
            }
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

                // Sinkronisasi ke REST API POST api/v1/config
                val bannerObj = BroadcastBanner(
                    id = "banner_${System.currentTimeMillis()}",
                    title = title,
                    message = message,
                    type = type,
                    isActive = isPublished
                )
                val request = UpdateConfigRequest(
                    announcement = message.ifBlank { title },
                    announcementType = type,
                    broadcastBanner = bannerObj
                )
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val token = AdminHelper.fetchAdminToken()
                        val payloadJson = Gson().toJson(request)
                        Log.d("ADMIN_SYNC", "POST /api/v1/config: payload=$payloadJson, tokenPresent=${token != null}")

                        var res = client.KomryuuxSource.updateAppConfig(authHeader = token, request = request)
                        if (res.code() == 405) {
                            Log.d("ADMIN_SYNC", "POST /api/v1/config returned 405, fallback ke PUT /api/v1/config")
                            res = client.KomryuuxSource.putAppConfig(authHeader = token, request = request)
                        }
                        Log.d("ADMIN_SYNC", "Response /api/v1/config: status=${res.code()}, isSuccess=${res.isSuccessful}, msg=${res.body()?.message}")

                        withContext(Dispatchers.Main) {
                            if (res.isSuccessful) {
                                val serverMsg = res.body()?.message ?: "Broadcast pengumuman berhasil disiarkan!"
                                Toast.makeText(context, serverMsg, Toast.LENGTH_SHORT).show()
                            } else {
                                val errBody = res.errorBody()?.string()
                                Log.e("ADMIN_SYNC", "showBroadcastDialog gagal HTTP ${res.code()}: $errBody")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ADMIN_SYNC", "Exception showBroadcastDialog: ${e.message}", e)
                    }
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
    // Theme Selector (Multi-Theme Switcher)
    // ----------------------------------------------------
    private fun setupThemeSelector() {
        val currentPreset = ThemeManager.getCurrentPreset(requireContext())

        binding.tvCurrentThemeBadge.text = currentPreset.displayName
        binding.tvActiveThemeDescription.text = currentPreset.description

        val presets = ThemeManager.getAllPresets()
        val adapter = AdapterThemePreset(
            presets = presets,
            activeKey = currentPreset.key
        ) { selectedPreset ->
            ThemeManager.switchTheme(requireActivity(), selectedPreset)
        }

        binding.rvThemes.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvThemes.adapter = adapter
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

    private fun syncAccountWithServer(user: FirebaseUser? = auth.currentUser) {
        val context = context ?: return
        val prefManager = PreferenceManager.getInstance(context)
        val uid = user?.uid ?: FirebaseAuth.getInstance().currentUser?.uid ?: prefManager.userUid

        if (uid.isNullOrBlank()) {
            Toast.makeText(context, "Silakan login terlebih dahulu untuk menyinkronkan data", Toast.LENGTH_SHORT).show()
            return
        }

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

                // 2. Ambil seluruh data dari LibraryDao dan riwayatDao di Dispatchers.IO
                val (syncLibrary, syncHistory) = withContext(Dispatchers.IO) {
                    val libList = db.libraryDao().getAllLibraryList().map { detail ->
                        SyncLibraryItem(
                            comicId = detail.comic.url.ifBlank { detail.comic.id.toString() },
                            title = detail.comic.title,
                            coverUrl = detail.comic.imageUrl,
                            sourceName = detail.comic.location,
                            updatedAt = detail.library.createdAt
                        )
                    }

                    val relasiHistory = db.riwayatdao().getAllRiwayatDetailList().map { detail ->
                        SyncHistoryItem(
                            comicId = detail.comic.url.ifBlank { detail.comic.id.toString() },
                            title = detail.comic.title,
                            coverUrl = detail.comic.imageUrl,
                            sourceName = detail.comic.location,
                            lastChapterNumber = detail.chapter.title,
                            lastPage = detail.riwayat.pageRead ?: 1,
                            updatedAt = detail.riwayat.readAt
                        )
                    }

                    val finalHistory = if (relasiHistory.isNotEmpty()) {
                        relasiHistory
                    } else {
                        val legacyDb = com.komik.komik.service.data.data.DatabaseRiwayat.getDatabase(context)
                        legacyDb.riwayatdao().getAllRiwayatList().map { r ->
                            SyncHistoryItem(
                                comicId = (r.urlWebsite ?: r.url ?: "").ifBlank { r.title },
                                title = r.title,
                                coverUrl = r.imageUrl ?: "",
                                sourceName = r.location,
                                lastChapterNumber = r.chapter,
                                lastPage = 1,
                                updatedAt = r.createdAt
                            )
                        }
                    }

                    Pair(libList, finalHistory)
                }

                // Cetak jumlah item yang akan dikirim ke Logcat
                Log.d("KOMRYUU_SYNC", "Sending library=${syncLibrary.size}, history=${syncHistory.size}")

                val syncReq = SyncRequest(
                    uid = uid,
                    library = syncLibrary,
                    history = syncHistory,
                    lastSync = System.currentTimeMillis()
                )

                val response = withContext(Dispatchers.IO) {
                    client.KomryuuxSource.syncData(syncReq)
                }

                Log.d("KOMRYUU_SYNC", "Response code=${response.code()}, message=${response.message()}")

                if (_binding == null) return@launch

                binding.progressSync.visibility = View.GONE
                binding.btnSyncNow.isEnabled = true
                binding.btnRestoreCloud.isEnabled = true

                if (response.isSuccessful) {
                    val body = response.body()
                    prefManager.lastSync = System.currentTimeMillis()
                    updateLastSyncDisplay(prefManager)

                    val libCount = body?.syncedLibraryCount ?: body?.data?.syncedLibraryCount ?: syncLibrary.size
                    val histCount = body?.syncedHistoryCount ?: body?.data?.syncedHistoryCount ?: syncHistory.size
                    val serverMsg = body?.message

                    val toastMsg = if (!serverMsg.isNullOrBlank()) {
                        "Sinkronisasi berhasil! $serverMsg ($libCount library, $histCount riwayat tersimpan)"
                    } else {
                        "Sinkronisasi berhasil! $libCount library dan $histCount riwayat tersimpan."
                    }
                    Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()

                    val snackbarMsg = "Sinkronisasi berhasil! $libCount library & $histCount riwayat tersimpan di Cloud."
                    Snackbar.make(binding.root, snackbarMsg, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(ContextCompat.getColor(context, R.color.bottom_navbar))
                        .show()
                } else {
                    val errorBodyStr = try {
                        response.errorBody()?.string()
                    } catch (_: Exception) {
                        null
                    }
                    val errorMsg = if (!errorBodyStr.isNullOrBlank()) {
                        try {
                            val jsonObject = org.json.JSONObject(errorBodyStr)
                            jsonObject.optString("message", errorBodyStr)
                        } catch (_: Exception) {
                            errorBodyStr
                        }
                    } else {
                        response.message().ifBlank { "Terjadi kesalahan pada server" }
                    }
                    val failureMsg = "Gagal sinkronisasi (HTTP ${response.code()}): $errorMsg"
                    Toast.makeText(context, failureMsg, Toast.LENGTH_LONG).show()
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

    private fun restoreAccountFromCloud(user: FirebaseUser? = auth.currentUser) {
        val context = context ?: return
        val prefManager = PreferenceManager.getInstance(context)
        val uid = user?.uid ?: FirebaseAuth.getInstance().currentUser?.uid ?: prefManager.userUid

        if (uid.isNullOrBlank()) {
            Toast.makeText(context, "Silakan login terlebih dahulu untuk memulihkan data", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("Pulihkan Data dari Cloud?")
            .setMessage("Koleksi komik dan riwayat baca Anda di server Komryuu.x akan diunduh dan digabungkan ke database lokal.")
            .setIcon(R.drawable.download_cloud_line)
            .setPositiveButton("Pulihkan Sekarang") { dialog, _ ->
                dialog.dismiss()
                executeRestore(uid)
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun executeRestore(uid: String) {
        val context = context ?: return
        binding.progressSync.visibility = View.VISIBLE
        binding.btnSyncNow.isEnabled = false
        binding.btnRestoreCloud.isEnabled = false
        Toast.makeText(context, "Mengunduh cadangan dari Cloud Komryuu.x...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    client.KomryuuxSource.restoreData(uid)
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

        // Floating Assistive Ball
        val prefManager = PreferenceManager.getInstance(requireContext())
        binding.switchFloatingBall.isChecked = prefManager.isFloatingReadBallEnabled
        binding.switchFloatingBall.setOnCheckedChangeListener { _, isChecked ->
            prefManager.isFloatingReadBallEnabled = isChecked
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

        // Auto Chapter Update Background Schedule
        setupAutoChapterUpdateSchedule()
    }

    private fun setupAutoChapterUpdateSchedule() {
        val context = context ?: return
        val prefManager = PreferenceManager.getInstance(context)

        // 1. Switch Utama
        binding.switchAutoUpdate.isChecked = prefManager.isAutoUpdateEnabled
        binding.switchAutoUpdate.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                    launcherPermissionNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            prefManager.isAutoUpdateEnabled = isChecked
            UpdateScheduler.schedulePeriodicCheck(context, prefManager.updateIntervalHours)
            updateIntervalUIState(isChecked)
        }

        // 2. Pemilih Interval
        updateIntervalDisplay(prefManager.updateIntervalHours)
        updateIntervalUIState(prefManager.isAutoUpdateEnabled)

        binding.rowUpdateInterval.setOnClickListener {
            if (!prefManager.isAutoUpdateEnabled) {
                Toast.makeText(context, "Aktifkan pengecekan otomatis terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showIntervalSelectionDialog()
        }

        binding.itemChapterUpdatesCenter.setOnClickListener {
            startActivity(Intent(requireContext(), com.komik.komik.ui.pages.updates.ChapterUpdatesActivity::class.java))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            db.chapterUpdateDao().getUnreadCount().collectLatest { unreadCount ->
                if (_binding != null) {
                    if (unreadCount > 0) {
                        binding.tvSettingUpdateBadge.visibility = View.VISIBLE
                        binding.tvSettingUpdateBadge.text = "$unreadCount Baru"
                    } else {
                        binding.tvSettingUpdateBadge.visibility = View.GONE
                    }
                }
            }
        }

        // 3. Status Terakhir Dicek & Tombol Cek Manual
        updateLastCheckTimeDisplay(prefManager.lastUpdateCheckMillis)

        binding.btnCheckUpdateNow.setOnClickListener {
            UpdateScheduler.triggerManualCheckNow(context)
            Toast.makeText(context, "Pengecekan chapter baru sedang berjalan di latar belakang...", Toast.LENGTH_SHORT).show()
            binding.tvLastUpdateCheck.text = "Terakhir dicek: Sedang memeriksa..."
        }
    }

    private fun updateIntervalUIState(isEnabled: Boolean) {
        binding.rowUpdateInterval.alpha = if (isEnabled) 1.0f else 0.5f
    }

    private fun updateIntervalDisplay(hours: Long) {
        val label = when (hours) {
            6L -> "Setiap 6 Jam"
            12L -> "Setiap 12 Jam (Disarankan)"
            24L -> "Setiap 24 Jam (1 Kali Sehari)"
            48L -> "Setiap 48 Jam (2 Hari Sekali)"
            else -> "Setiap $hours Jam"
        }
        binding.tvCurrentInterval.text = label
    }

    private fun updateLastCheckTimeDisplay(millis: Long) {
        if (millis > 0L) {
            val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(millis))
            binding.tvLastUpdateCheck.text = "Terakhir dicek: $dateStr"
        } else {
            binding.tvLastUpdateCheck.text = "Terakhir dicek: Belum pernah dicek"
        }
    }

    private fun showIntervalSelectionDialog() {
        val context = context ?: return
        val prefManager = PreferenceManager.getInstance(context)

        val options = arrayOf(
            "Setiap 6 Jam",
            "Setiap 12 Jam (Disarankan — Hemat Server & Baterai)",
            "Setiap 24 Jam (1 Kali Sehari)",
            "Setiap 48 Jam (2 Hari Sekali)"
        )
        val hoursList = listOf(6L, 12L, 24L, 48L)

        val currentHours = prefManager.updateIntervalHours
        val selectedIndex = hoursList.indexOf(currentHours).let { if (it >= 0) it else 1 }

        MaterialAlertDialogBuilder(context)
            .setTitle("Pilih Interval Pengecekan")
            .setIcon(R.drawable.baseline_history_24)
            .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                val selectedHours = hoursList[which]
                prefManager.updateIntervalHours = selectedHours
                updateIntervalDisplay(selectedHours)
                UpdateScheduler.schedulePeriodicCheck(context, selectedHours)
                Toast.makeText(context, "Interval diubah menjadi ${options[which]}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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
        binding.tvAppVersion.text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) • Ketuk untuk cek pembaruan"

        binding.itemCheckAppUpdate.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                AppUpdateChecker.checkForUpdate(requireContext(), isManual = true)
            }
        }

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
        val context = requireContext()
        val pref = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        pref.edit().putBoolean("notif", granted).apply()
        binding.Notification.isChecked = granted
        binding.switchAutoUpdate.isChecked = granted
        val prefManager = PreferenceManager.getInstance(context)
        prefManager.isAutoUpdateEnabled = granted
        UpdateScheduler.schedulePeriodicCheck(context, prefManager.updateIntervalHours)
        if (!granted) {
            Toast.makeText(context, "Izin Notifikasi Ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        setupUserProfile()
        (activity as? com.komik.komik.MainActivity)?.refreshNavProfileAvatar()
        calculateImageCacheSize()
        val context = context
        if (context != null) {
            val prefManager = PreferenceManager.getInstance(context)
            updateLastCheckTimeDisplay(prefManager.lastUpdateCheckMillis)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adminFirestoreListener?.remove()
        adminUsersCountListener?.remove()
        _binding = null
    }

    fun formatDeviceInfo(raw: Any?): String = SettingFragment.formatDeviceInfo(raw)
    fun formatReportDate(raw: Any?): String = SettingFragment.formatReportDate(raw)
    fun formatLastLogin(raw: Any?): String = SettingFragment.formatLastLogin(raw)
    fun isUserActive24h(raw: Any?): Boolean = SettingFragment.isUserActive24h(raw)
    fun parseDateToMillis(raw: Any?): Long = SettingFragment.parseDateToMillis(raw)

    fun showReportDetailDialog(report: ReportItem) {
        val ctx = context ?: return
        val deviceFormatted = formatDeviceInfo(report.deviceInfo)
        val reporter = report.reporterEmail ?: "Anonim"
        val statusText = report.status.uppercase()
        val comic = report.comicTitle.ifBlank { "Komik Tidak Disebutkan" }
        val chapter = report.chapterTitle?.let { "\nChapter: $it" } ?: ""
        val category = report.category ?: "Laporan Masalah"
        val desc = report.description.ifBlank { "(Tidak ada deskripsi)" }
        val adminNote = report.adminNote?.let { "\nCatatan Admin: $it" } ?: ""

        MaterialAlertDialogBuilder(ctx)
            .setTitle("Detail Tiket Laporan")
            .setMessage("Status: $statusText\nKategori: $category\nKomik: $comic$chapter\nPelapor: $reporter\nPerangkat: $deviceFormatted\n\nPesan: $desc$adminNote")
            .setPositiveButton("Tutup", null)
            .show()
    }

    fun showUserDetailDialog(user: AdminUserModel) {
        val ctx = context ?: return
        SettingFragment.showUserDetailDialog(ctx, user)
    }

    companion object {
        fun formatDeviceInfo(raw: Any?): String {
            if (raw == null) return "-"
            if (raw is String) return if (raw.isBlank()) "-" else raw
            if (raw is Map<*, *>) {
                val brand = raw["brand"]?.toString() ?: ""
                val model = raw["model"]?.toString() ?: ""
                val os = raw["androidVersion"]?.toString()
                    ?: raw["os"]?.toString()
                    ?: raw["osVersion"]?.toString()
                    ?: raw["version"]?.toString()
                    ?: "-"
                val appVersion = raw["appVersion"]?.toString() ?: raw["versionName"]?.toString()
                val base = "$brand $model (Android $os)".trim()
                return if (base.isNotBlank() && base != "(Android -)") {
                    if (!appVersion.isNullOrBlank()) "$base, v$appVersion" else base
                } else {
                    raw.entries.filter { it.value != null }
                        .joinToString(" • ") { "${it.key}: ${it.value}" }
                        .ifBlank { "-" }
                }
            }
            if (raw is com.google.gson.JsonObject) {
                val brand = raw.get("brand")?.asString ?: ""
                val model = raw.get("model")?.asString ?: ""
                val os = raw.get("androidVersion")?.asString
                    ?: raw.get("os")?.asString
                    ?: raw.get("osVersion")?.asString
                    ?: raw.get("version")?.asString
                    ?: "-"
                val appVersion = raw.get("appVersion")?.asString ?: raw.get("versionName")?.asString
                val base = "$brand $model (Android $os)".trim()
                return if (base.isNotBlank() && base != "(Android -)") {
                    if (!appVersion.isNullOrBlank()) "$base, v$appVersion" else base
                } else {
                    raw.entrySet().joinToString(" • ") { "${it.key}: ${it.value}" }.ifBlank { "-" }
                }
            }
            return raw.toString()
        }

        fun formatReportDate(raw: Any?): String {
            return com.komik.komik.service.utils.formatReportDate(raw)
        }

        fun parseDateToMillis(raw: Any?): Long {
            if (raw == null) return 0L
            if (raw is Number) {
                val num = raw.toLong()
                return if (num > 0L && num < 100_000_000_000L) num * 1000L else num
            }
            if (raw is com.google.firebase.Timestamp) {
                return raw.toDate().time
            }
            if (raw is Date) {
                return raw.time
            }
            val str = raw.toString().trim()
            if (str.isEmpty()) return 0L

            val num = str.toDoubleOrNull()
            if (num != null) {
                val l = num.toLong()
                return if (l > 0L && l < 100_000_000_000L) l * 1000L else l
            }

            val isoPatterns = arrayOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
            )

            for (pattern in isoPatterns) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val date = sdf.parse(str)
                    if (date != null) {
                        return date.time
                    }
                } catch (_: Exception) {
                }
            }
            return 0L
        }

        fun isUserActive24h(raw: Any?): Boolean {
            val millis = parseDateToMillis(raw)
            if (millis <= 0L) return false
            val diff = System.currentTimeMillis() - millis
            return diff in 0L..86_400_000L
        }

        fun formatLastLogin(raw: Any?): String {
            val millis = parseDateToMillis(raw)
            if (millis <= 0L) return "Belum tercatat"

            val now = System.currentTimeMillis()
            val diff = now - millis

            if (diff < 0L) return "Baru saja"
            if (diff < 60_000L) return "Baru saja"
            if (diff < 3600_000L) {
                val minutes = (diff / 60_000L).coerceAtLeast(1)
                return "$minutes menit lalu"
            }

            val locale = Locale("id", "ID")
            val nowCal = Calendar.getInstance()
            val thenCal = Calendar.getInstance().apply { timeInMillis = millis }

            val isToday = nowCal.get(Calendar.YEAR) == thenCal.get(Calendar.YEAR) &&
                    nowCal.get(Calendar.DAY_OF_YEAR) == thenCal.get(Calendar.DAY_OF_YEAR)

            return if (isToday) {
                val timeFormat = SimpleDateFormat("HH:mm", locale).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Jakarta")
                }
                "Hari ini, ${timeFormat.format(Date(millis))}"
            } else {
                val isThisYear = nowCal.get(Calendar.YEAR) == thenCal.get(Calendar.YEAR)
                val pattern = if (isThisYear) "dd MMM, HH:mm" else "dd MMM yyyy, HH:mm"
                val fullFormat = SimpleDateFormat(pattern, locale).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Jakarta")
                }
                fullFormat.format(Date(millis))
            }
        }

        fun showUserDetailDialog(context: Context, user: AdminUserModel) {
            val lastLoginStr = formatLastLogin(user.lastLogin)
            val regDateStr = formatReportDate(user.createdAt)
            val activeStatus = if (user.isBanned) "Dibanned: ${user.banReason}" else if (isUserActive24h(user.lastLogin)) "Aktif (Online 24 Jam)" else "Offline"
            val vipStatus = if (user.role.equals("vip", true)) {
                if (user.vipExpiresAt <= 0L) "VIP (Permanen)" else "VIP hingga ${formatReportDate(user.vipExpiresAt)}"
            } else {
                user.role.uppercase()
            }

            val msg = buildString {
                append("Nama: ${user.name}\n")
                append("Email: ${user.email.ifBlank { "(Tanpa email)" }}\n")
                append("UID: ${user.uid}\n")
                append("Role: $vipStatus\n")
                append("Status: $activeStatus\n")
                append("Terakhir Masuk: $lastLoginStr\n")
                append("Terdaftar: $regDateStr")
            }

            MaterialAlertDialogBuilder(context)
                .setTitle("Detail Pengguna")
                .setMessage(msg)
                .setPositiveButton("Tutup", null)
                .show()
        }
    }
}
