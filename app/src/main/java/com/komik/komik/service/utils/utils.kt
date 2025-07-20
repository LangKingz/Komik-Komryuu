package com.komik.komik.service.utils

import android.annotation.SuppressLint
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

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
    // 1. tfv: Timestamp saat ini dalam milidetik
    val tfv = Instant.now().toEpochMilli()

    // 2. theme: Kita bisa set default 'mdark' atau random
    val theme = "mdark" // Bisa juga Random.nextBoolean() ? "mdark" : "mlight"

    // 3. _sharedID: UUID (Universally Unique Identifier) untuk ID yang unik banget
    val sharedID = UUID.randomUUID().toString()

    // 4. _ym_uid: Angka acak yang sangat besar, mirip timestamp atau ID numerik
    // Kita bisa gabungkan timestamp + random number
    val ymUid = "${Instant.now().toEpochMilli()}${Random.nextLong(1000000000000000L, 9999999999999999L)}"

    // 5. _ym_uid_cst: String unik yang di-base64 encode
    // Contoh: encode UUID acak
    val ymUidCstRaw = UUID.randomUUID().toString().take(10) // ambil 10 karakter pertama biar ga kepanjangan
    val ymUidCst = Base64.getEncoder().encodeToString(ymUidCstRaw.toByteArray()).replace("=", "") // hilangkan padding '='

    // 6. _lr_env_src_ats: Boolean random
    val lrEnvSrcAts = Random.nextBoolean()

    // 7. _sharedID_cst: Mirip _ym_uid_cst, string unik di-base64 encode
    val sharedIDPstRaw = UUID.randomUUID().toString().take(10)
    val sharedIDPst = Base64.getEncoder().encodeToString(sharedIDPstRaw.toByteArray()).replace("=", "")

    // 8. wd: Dimensi jendela acak
    val width = Random.nextInt(1000, 1920) // Lebar antara 1000 dan 1920
    val height = Random.nextInt(500, 1080) // Tinggi antara 500 dan 1080
    val wd = "${width}x${height}"

    // Gabungkan semua jadi satu string cookie
    return "tfv=$tfv; theme=$theme; _sharedID=$sharedID; _ym_uid=$ymUid; _ym_uid_cst=$ymUidCst; _lr_env_src_ats=$lrEnvSrcAts; _sharedID_cst=$sharedIDPst; wd=$wd"
}
