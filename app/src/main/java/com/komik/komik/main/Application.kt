package com.komik.komik.main

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.komik.komik.service.utils.PreferenceManager
import com.komik.komik.service.utils.RemoteSourceConfigManager
import com.komik.komik.service.utils.generateFakeCookie
import com.komik.komik.service.worker.AutoSyncScheduler
import com.komik.komik.service.worker.UpdateScheduler

class ApplicationAndroid : Application() {

    override fun onCreate() {
        super.onCreate()
        RemoteSourceConfigManager.init(this)
        checkNotif()
        genereteCookie()
        settingView()

        // Inisialisasi jadwal pengecekan chapter otomatis di background
        val prefManager = PreferenceManager.getInstance(this)
        UpdateScheduler.schedulePeriodicCheck(this, prefManager.updateIntervalHours)

        // Inisialisasi jadwal sinkronisasi cloud otomatis di background
        AutoSyncScheduler.scheduleAutoSync(this)
    }

    private fun checkNotif(){
        val pref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val edit = pref.edit()
        when{
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                edit.putBoolean("notif",true)
            }

            else -> {
                edit.putBoolean("notif",false)
            }
        }
        edit.apply()
    }

    private fun genereteCookie(){
        val cookie = generateFakeCookie()

        val pref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val editor = pref.edit()

        editor.putString("cookie",cookie)
        editor.apply()
    }

    private fun settingView(){
        val pref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val edit = pref.edit()

        val safe = pref.getBoolean("safemode",false)

        edit.putBoolean("fullscreen",true)
        edit.putBoolean("update",false)
        edit.putBoolean("pindah",true)
        edit.putBoolean("safemode",safe)
        edit.apply()

    }
}
