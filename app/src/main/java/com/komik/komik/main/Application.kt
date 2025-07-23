package com.komik.komik.main

import android.app.Application
import com.komik.komik.service.utils.generateFakeCookie

class ApplicationAndroid : Application() {

    override fun onCreate() {
        super.onCreate()
        genereteCookie()
    }

    private fun genereteCookie(){
        val cookie = generateFakeCookie()

        val pref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val editor = pref.edit()

        editor.putString("cookie",cookie)
        editor.apply()
    }
}