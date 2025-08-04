package com.komik.komik.service.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import java.util.Date

fun downloadImageManager(context:Context,url:String){
    val filename = "gambar_${System.currentTimeMillis()}_komryu.jpg"

//    request download
    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle(filename)
        .setDescription("Mengunduh Gambar")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,filename)
        .setAllowedOverMetered(true) // data seluler
        .setAllowedOverRoaming(true) // roaming

    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    downloadManager.enqueue(request)

    Toast.makeText(context, "Mengunduh Gambar....", Toast.LENGTH_SHORT).show()
}