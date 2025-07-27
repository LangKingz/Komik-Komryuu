package com.komik.komik.main

import android.app.Application
import com.komik.komik.service.utils.generateFakeCookie

class ApplicationAndroid : Application() {

    override fun onCreate() {
        super.onCreate()
        genereteCookie()
        settingView()
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

        edit.putBoolean("fullscreen",true)
        edit.putBoolean("update",false)
        edit.putBoolean("pindah",true)
        edit.apply()

    }
}