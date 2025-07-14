package com.komik.komik.service.utils

import java.util.concurrent.TimeUnit

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