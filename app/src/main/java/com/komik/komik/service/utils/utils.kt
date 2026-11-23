package com.komik.komik.service.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.view.WindowCompat
import com.komik.komik.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

// === FUNGSI PEMBANTU BARU UNTUK EKSTRAKSI & FORMAT ANGKA CHAPTER ===
fun extractNumberFromDataNum(dataNumString: String): Double {
    // Regex: \d+(\.\d+)?
    // \d+   : Mencocokkan satu atau lebih digit (0-9)
    // (\.\d+)?: Opsional (?), cocokkan satu titik (.) diikuti satu atau lebih digit
    // Ini akan menangkap "314", "316", "7.1", "8.2", dll.
    val regex = Regex("""\d+(\.\d+)?""")

    val matchResult = regex.find(dataNumString) // Cari pola angka di dalam string `dataNumString`

    // Ambil nilai yang cocok (misalnya "8.2" dari "Chapter 8.2")
    val numberString = matchResult?.value

    // Konversi ke Double. Jika tidak ada angka yang cocok atau konversi gagal, return 0.0.
    return numberString?.toDoubleOrNull() ?: 0.0
}

fun Double.formatChapterNumber(): String {
    return if (this % 1.0 == 0.0) {
        this.toLong().toString()
    } else {
        // Hilangkan trailing zeros jika ada, tapi pertahankan desimal (misal 8.2, 7.1)
        val formatted = this.toString()
        if (formatted.endsWith(".0")) formatted.substringBefore(".0") else formatted
    }
}

fun Double?.formatChapterNumberOrEmpty(): String {
    if (this == null) return ""
    return this.formatChapterNumber()
}

fun formatWaktuLalu(createdAt: Long): String {
    val sekarang = System.currentTimeMillis()
    if (createdAt > sekarang || createdAt <= 0) {
        return "Baru saja"
    }

    val selisih = sekarang - createdAt

    return when {
        selisih < TimeUnit.MINUTES.toMillis(1) -> "Baru saja"
        selisih < TimeUnit.HOURS.toMillis(1) -> {
            val menit = TimeUnit.MILLISECONDS.toMinutes(selisih)
            "$menit menit yang lalu"
        }

        selisih < TimeUnit.DAYS.toMillis(1) -> {
            val jam = TimeUnit.MILLISECONDS.toHours(selisih)
            "$jam jam yang lalu"
        }

        selisih < TimeUnit.DAYS.toMillis(2) -> "Kemarin"
        selisih < TimeUnit.DAYS.toMillis(7) -> {
            val hari = TimeUnit.MILLISECONDS.toDays(selisih)
            "$hari hari yang lalu"
        }

        else -> {
            // Jika sudah lebih dari seminggu, tampilkan tanggal saja
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID"))
            sdf.format(java.util.Date(createdAt))
        }
    }
}

// Fungsi untuk membuat fake cookie
@SuppressLint("NewApi")
fun generateFakeCookie(): String {
    val tfv = Instant.now().toEpochMilli()

    // 2. theme: Kita bisa set default 'mdark' atau random
    val theme = "mdark" // Bisa juga Random.nextBoolean() ? "mdark" : "mlight"

    // 3. _sharedID: UUID (Universally Unique Identifier) untuk ID yang unik banget
    val sharedID = UUID.randomUUID().toString()

    // 4. _ym_uid: Angka acak yang sangat besar, mirip timestamp atau ID numerik
    // Kita bisa gabungkan timestamp + random number
    val ymUid =
        "${Instant.now().toEpochMilli()}${Random.nextLong(1000000000000000L, 9999999999999999L)}"

    // 5. _ym_uid_cst: String unik yang di-base64 encode
    // Contoh: encode UUID acak
    val ymUidCstRaw =
        UUID.randomUUID().toString().take(10) // ambil 10 karakter pertama biar ga kepanjangan
    val ymUidCst = Base64.getEncoder().encodeToString(ymUidCstRaw.toByteArray())
        .replace("=", "") // hilangkan padding '='

    // 6. _lr_env_src_ats: Boolean random
    val lrEnvSrcAts = Random.nextBoolean()

    // 7. _sharedID_cst: Mirip _ym_uid_cst, string unik di-base64 encode
    val sharedIDPstRaw = UUID.randomUUID().toString().take(10)
    val sharedIDPst =
        Base64.getEncoder().encodeToString(sharedIDPstRaw.toByteArray()).replace("=", "")

    // 8. wd: Dimensi jendela acak
    val width = Random.nextInt(1000, 1920) // Lebar antara 1000 dan 1920
    val height = Random.nextInt(500, 1080) // Tinggi antara 500 dan 1080
    val wd = "${width}x${height}"

    // Gabungkan semua jadi satu string cookie
    return "tfv=$tfv; theme=$theme; _sharedID=$sharedID; _ym_uid=$ymUid; _ym_uid_cst=$ymUidCst; _lr_env_src_ats=$lrEnvSrcAts; _sharedID_cst=$sharedIDPst; wd=$wd"
}

suspend fun downloadAndSaveImage(context: Context, imageUrl: String, id: Long): String? {
    return withContext(Dispatchers.IO) {
        try {
            val fileName = "comic_${id}_image.jpg" // Nama file unik
            val file = File(context.filesDir, fileName) // Simpan di direktori internal aplikasi

            val url = URL(imageUrl)
            val connection = url.openConnection()
            connection.connect()

            val inputStream: InputStream = connection.getInputStream()
            val outputStream = FileOutputStream(file)

            inputStream.copyTo(outputStream) // Copy byte dari input ke output stream

            outputStream.close()
            inputStream.close()

            file.absolutePath // Mengembalikan path file lokal
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ImageDownloader", "Gagal mengunduh dan menyimpan gambar: ${e.localizedMessage}")
            null
        }
    }
}

//fun compatMode (context: Context){
//    window.navigationBarColor = resources.getColor(R.color.background_utama)
//
//    WindowCompat.getInsetsController(window, window.decorView).let { controller ->
//        controller.isAppearanceLightStatusBars = false
//        controller.isAppearanceLightNavigationBars = false
//    }
//}

fun genereteId(): String {

    val timestamp = System.currentTimeMillis()
    val randomString = UUID.randomUUID().toString().substring(0, 8)

    return "$timestamp"

}


fun parsechapterToInt(chapter: String): Double {
    val regex = Regex("\\d+")
    val matchResult = regex.find(chapter)

    if (matchResult != null) {
        return matchResult.value.toDouble()
    }
    return 0.0
}


fun parseDate(dateString: String): String {
    // 1. Definisikan format INPUT sesuai dengan string yang masuk
    // yyyy-MM-dd'T'HH:mm:ssX -> Ini adalah format ISO 8601
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())

    // 2. Definisikan format OUTPUT yang kamu inginkan
    val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    try {
        // 3. Parse string input menjadi sebuah objek Date
        val dateObject = inputFormat.parse(dateString)

        // 4. Format objek Date tadi menjadi string dengan format baru
        // Pastikan dateObject tidak null sebelum memformat
        return if (dateObject != null) {
            outputFormat.format(dateObject)
        } else {
            "Invalid Date"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return "Error Parsing Date" // Kembalikan pesan error jika parsing gagal
    }
}

fun isNetworkAvaible (context: Context):Boolean{
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    val network= connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
}
