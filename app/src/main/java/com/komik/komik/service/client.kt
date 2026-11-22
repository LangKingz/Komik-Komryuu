package com.komik.komik.service

import android.content.Context
import com.google.gson.GsonBuilder
import com.komik.komik.service.Scrape.DoujindesuDecryptor
import com.komik.komik.service.api.DoujinDesuService
import com.komik.komik.service.api.IkiruService
import com.komik.komik.service.api.Scrape
import com.komik.komik.service.api.ScrapeComicscan
import com.komik.komik.service.api.ScrapeIkiru
import com.komik.komik.service.api.ScrapeKomikcast
import com.komik.komik.service.api.ShinigamiService
import com.komik.komik.service.api.SourceService
import com.komik.komik.service.api.newSourceService
import com.komik.komik.service.utils.RemoteSourceConfigManager
import com.komik.komik.service.utils.generateFakeCookie
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object client {

    private const val URLSHINIGAMIAPI = "https://api.shinigami.io/"
    private const val URLSOURCE = "https://www.komryuu.my.id/"
    private const val IkiruUrl = "https://11.ikiru.wtf/"
    private const val KomikcastUrl = "https://komikcast.li/"
    private const val ComicscanUrl = "https://lc3.cosmicscans.asia/"

    private const val Komryuux = "https://komryuu-x.vercel.app/"

    private const val DoujindesuUrl = "https://doujin.desu.xxx/"

    // ----------------------------------------------------
    // Dynamic Header & Mirror Fallback Interceptor
    // ----------------------------------------------------
    private val dynamicHeaderInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val urlStr = originalRequest.url.toString()
        val host = originalRequest.url.host

        val customHeaders = RemoteSourceConfigManager.getCustomHeadersForUrl(urlStr)
            ?: RemoteSourceConfigManager.getCustomHeadersForHost(host)

        val requestBuilder = originalRequest.newBuilder()

        if (customHeaders != null) {
            if (!customHeaders.userAgent.isNullOrBlank()) {
                requestBuilder.header("User-Agent", customHeaders.userAgent)
            }
            if (!customHeaders.referer.isNullOrBlank()) {
                requestBuilder.header("Referer", customHeaders.referer)
            }
            if (!customHeaders.cookie.isNullOrBlank()) {
                requestBuilder.header("Cookie", customHeaders.cookie)
            }
        }

        try {
            val response = chain.proceed(requestBuilder.build())
            if (response.isSuccessful) {
                RemoteSourceConfigManager.markUrlSuccess(urlStr)
            } else if (response.code in listOf(500, 502, 503, 504)) {
                RemoteSourceConfigManager.markUrlFailed(urlStr)
            }
            response
        } catch (e: Exception) {
            RemoteSourceConfigManager.markUrlFailed(urlStr)
            throw e
        }
    }

    fun getActiveUrl(sourceKey: String, defaultUrl: String): String {
        return RemoteSourceConfigManager.getActiveUrl(sourceKey, defaultUrl)
    }

    fun getIkiruUrl(): String = getActiveUrl("ikiru", IkiruUrl)
    fun getDoujindesuUrl(): String = getActiveUrl("doujindesu", DoujindesuUrl)
    fun getKomikcastUrl(): String = getActiveUrl("komikcast", KomikcastUrl)
    fun getComicscanUrl(): String = getActiveUrl("comicscan", ComicscanUrl)
    fun getShinigamiUrl(): String {
        val configured = getActiveUrl("shinigami", URLSHINIGAMIAPI)
        // ShinigamiService Retrofit memerlukan endpoint REST API (mengandung "api.").
        // Jika server konfigurasi mengirim domain web (misal: "https://07.shinigami.asia/"),
        // domain web tersebut akan mengembalikan HTML 301 / redirect yang menyebabkan Gson MalformedJsonException.
        if (configured.contains("api.", ignoreCase = true) || configured.contains("api-", ignoreCase = true)) {
            return configured
        }
        val entry = RemoteSourceConfigManager.findConfig("shinigami")
        val apiMirror = entry?.mirrorUrls?.firstOrNull { it.contains("api.", ignoreCase = true) }
        return apiMirror ?: URLSHINIGAMIAPI
    }

    val ikiruService: IkiruService by lazy {
        Retrofit.Builder()
            .baseUrl(getIkiruUrl())
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(dynamicHeaderInterceptor)
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header(
                                "User-Agent",
                                "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1"
                            )
                            .header(
                                "Accept",
                                "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"
                            )
                            .build()
                        chain.proceed(request)
                    }
                    .build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(IkiruService::class.java)
    }

    val DoujinDesuServiceDyricpted: DoujinDesuService by lazy {
        Retrofit.Builder()
            .baseUrl(getDoujindesuUrl())
            .client(
                OkHttpClient.Builder()
                .addInterceptor(dynamicHeaderInterceptor)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        )
                        .header("Referer", "https://doujin.desu.xxx/")
                        .header("Accept", "application/json, text/plain, */*")
                        .build()
                    chain.proceed(request)
                }
                .build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DoujinDesuService::class.java)
    }

    val Doujindesu: ScrapeIkiru by lazy {
        Retrofit.Builder()
            .baseUrl(getDoujindesuUrl())
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(dynamicHeaderInterceptor)
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header(
                                "User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                            )
                            .header("Referer", "https://doujin.desu.xxx/")
                            .header("Accept", "application/json, text/plain, */*")
                            .build()

                        val response = chain.proceed(request)
                        val rawString = response.body?.string() ?: ""

                        val decryptedString = DoujindesuDecryptor.decryptIfNeeded(rawString)

                        response.newBuilder()
                            .body(decryptedString.toResponseBody(response.body?.contentType()))
                            .build()
                    }
                    .build()
            )
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(ScrapeIkiru::class.java)
    }

    val Shinigami: ShinigamiService by lazy {
        val gson = GsonBuilder()
            .setLenient()
            .create()

        Retrofit.Builder()
            .baseUrl(getShinigamiUrl())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(dynamicHeaderInterceptor)
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header(
                                "User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                            )
                            .header(
                                "Referer", "https://api.shinigami.io/"
                            )
                            .header(
                                "Origin", "https://api.shinigami.io/"
                            ).header(
                                "Accept",
                                "application/json, text/plain, */*"
                            )
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            )
            .build()
            .create(ShinigamiService::class.java)
    }

    const val API_KEY = "91f30a30adcb829df7bb8d2146fef5b3104951838996284d24392a02eef83b96"

    val Source: SourceService by lazy {
        Retrofit.Builder()
            .baseUrl(URLSOURCE)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header("x-api-key", API_KEY)
                            .header("Accept", "application/json")
                            .header("Content-Type", "application/json")
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SourceService::class.java)
    }

    val Ikiru: ScrapeIkiru by lazy {
        Retrofit.Builder()
            .baseUrl(getIkiruUrl())
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(dynamicHeaderInterceptor)
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header(
                                "User-Agent",
                                "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1"
                            )
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            )
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(ScrapeIkiru::class.java)
    }

    val Komikcast: ScrapeKomikcast by lazy {
        Retrofit.Builder()
            .baseUrl(getKomikcastUrl())
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(dynamicHeaderInterceptor)
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header(
                                "User-Agent",
                                "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1"
                            )
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            )
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(ScrapeKomikcast::class.java)
    }

    val Comicscan: ScrapeComicscan by lazy {
        Retrofit.Builder()
            .baseUrl(getComicscanUrl())
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(dynamicHeaderInterceptor)
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header(
                                "User-Agent",
                                "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1"
                            )
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            )
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(ScrapeComicscan::class.java)
    }

    val cookie = generateFakeCookie()

    val ScrapeDataImage: Scrape by lazy {
        Retrofit.Builder()
            .baseUrl(getIkiruUrl())
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(dynamicHeaderInterceptor)
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header(
                                "User-Agent",
                                "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1"
                            )
                            .header(
                                "Cookie",
                                cookie
                            )
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            )
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(Scrape::class.java)
    }

    val KomryuuxSource: newSourceService by lazy {
        Retrofit.Builder()
            .baseUrl(Komryuux)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header(
                                "User-Agent",
                                "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1"
                            )
                            .header(
                                "x-api-key",
                                API_KEY
                            )
                            .header("Accept", "application/json")
                            .header("Content-Type", "application/json")
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(newSourceService::class.java)
    }

}
