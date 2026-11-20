package com.komik.komik.service.Scrape

import com.komik.komik.service.client
import com.komik.komik.service.parser.model.getImages
import com.komik.komik.service.parser.model.getListUpdate
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.math.abs



object ResponseDecryptor {
    private const val SALT = "doujindesu-scrapers-cannot-read-this-super-secret-salt-2026-v2"
    private const val HOUR_IN_MS = 3_600_000L

    // Equivalent dengan fungsi sn(t) di JS
    private fun generateKey(timeSlot: Long): String {
        val input = "${SALT}_$timeSlot"
        var hash = 0
        for (char in input) {
            // Di JS: r = (r << 5) - r + n.charCodeAt(l); r |= 0;
            // Tipe Int di Kotlin secara otomatis 32-bit signed integer seperti `| 0` di JS
            hash = (hash shl 5) - hash + char.code
        }

        val sb = StringBuilder()
        // Gunakan Long agar tidak overflow saat perkalian LCG (1664525)
        var seed = if (hash == 0) 123456789L else abs(hash.toLong())

        for (i in 0 until 32) {
            seed = (seed * 1664525L + 1013904223L) % 4294967296L
            val charCode = 33 + (seed % 93).toInt()
            sb.append(charCode.toChar())
        }
        return sb.toString()
    }

    // Equivalent dengan fungsi cl() di JS (menghasilkan 3 kemungkinan key)
    private fun getCandidateKeys(customTimestampMs: Long = System.currentTimeMillis()): List<String> {
        val currentSlot = customTimestampMs / HOUR_IN_MS
        return listOf(
            generateKey(currentSlot),
            generateKey(currentSlot - 1),
            generateKey(currentSlot + 1)
        )
    }

    // Equivalent dengan fungsi Ro(t, n) di JS
    private fun xorDecrypt(hexCipher: String, key: String): String {
        val cleanHex = hexCipher.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
        val len = cleanHex.length / 2
        val bytes = IntArray(len)
        for (i in 0 until len) {
            val hexPair = cleanHex.substring(i * 2, i * 2 + 2)
            bytes[i] = hexPair.toIntOrNull(16) ?: 0
        }

        val result = StringBuilder()
        val keyLen = key.length
        var rolling = 42

        for (i in bytes.indices) {
            val b = bytes[i]
            val k = key[i % keyLen].code
            val decryptedChar = (b xor k xor (i * 13) xor rolling) and 255
            result.append(decryptedChar.toChar())
            rolling = (rolling + b) % 256
        }
        return result.toString()
    }

    // Equivalent dengan fungsi dl(t, n) di JS dengan mencoba semua kandidat key
    fun decryptResponse(encryptedHex: String, timestampMs: Long = System.currentTimeMillis()): String {
        val keys = getCandidateKeys(timestampMs)

        for (key in keys) {
            try {
                val rawDecoded = xorDecrypt(encryptedHex, key)
                // Di JS: decodeURIComponent(r)
                val jsonString = URLDecoder.decode(rawDecoded, StandardCharsets.UTF_8.name())

                // Cek validitas awal apakah hasilnya berbentuk JSON
                if (jsonString.trim().startsWith("{") || jsonString.trim().startsWith("[")) {
                    return jsonString
                }
            } catch (e: Exception) {
                // Jika key salah, URLDecoder biasanya gagal karena karakter tidak valid, lanjut ke key berikutnya
            }
        }
        return "Gagal mendekripsi. Pastikan timestamp sesuai dengan waktu saat API di-request."
    }
}

object DoujindesuDecryptor {
    fun decryptIfNeeded(rawString: String): String {
        return try {
            if (rawString.contains("_enc_resp_")) {
                val json = JSONObject(rawString)
                val encResp = json.optString("_enc_resp_")
                if (encResp.isNotEmpty()) {
                    ResponseDecryptor.decryptResponse(encResp)
                } else {
                    rawString
                }
            } else {
                rawString
            }
        } catch (_: Exception) {
            rawString
        }
    }
}

fun main() {
    // Masukkan string hex dari "_enc_resp_" di sini
    val encResp = "5ae468b3de4ebf20e9285111df4496024077eca318233e3f9042ca4e8129897e9835f4892c3245ff44e6568864e8d527ea4c3336d19f57032cdaed145b41b90e4a"

    // CATATAN PENTING:
    // Key bergantung pada JAM saat request API dilakukan (System.currentTimeMillis() / 3600000).
    // Jika `_enc_resp_` diambil baru saja (< 1 jam), biarkan default.
    // Jika diambil beberapa jam/hari lalu, masukkan perkiraan timestamp (dalam milidetik) saat data itu diambil.
    println("Hasil Dekripsi:")
    try {
        val hasilJson = ResponseDecryptor.decryptResponse(encResp)
        println(hasilJson)
    } catch (e: Exception) {
        println("Error saat dekripsi: ${e.message}")
    }
}
