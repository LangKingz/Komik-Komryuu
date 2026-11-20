package com.komik.komik.service

import android.content.Context
import com.komik.komik.service.Scrape.DoujindesuDecryptor
import com.komik.komik.service.api.Scrape
import com.komik.komik.service.api.ScrapeComicscan
import com.komik.komik.service.api.ScrapeIkiru
import com.komik.komik.service.api.ScrapeKomikcast
import com.komik.komik.service.api.ShinigamiService
import com.komik.komik.service.api.SourceService
import com.komik.komik.service.api.newSourceService
import com.komik.komik.service.utils.generateFakeCookie
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object client {

    private const val URLSHINIGAMIAPI = "https://api.shinigami.io/"
    private const val URLSOURCE = "https://www.komryuu.my.id/"
    private const val IkiruUrl = "https://id.ikiru.wtf/"
    private const val KomikcastUrl = "https://komikcast.li/"
    private const val ComicscanUrl = "https://lc3.cosmicscans.asia/"

    private const val Komryuux ="https://komryuu-x.vercel.app/"

    private const val DoujindesuUrl = "https://doujin.desu.xxx/"

    val Doujindesu: ScrapeIkiru by lazy {
        Retrofit.Builder()
            .baseUrl(DoujindesuUrl)
            .client(
                OkHttpClient.Builder()
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

                        // Otomatis dekripsi kalau response-nya dibungkus _enc_resp_
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
        Retrofit.Builder()
            .baseUrl(URLSHINIGAMIAPI)
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1"
                        )
                        .header(
                            "Referer", "https://s1.shinigami.mx/"
                        )
                        .header(
                            "Origin", "https://s1.shinigami.mx/"
                        ).header(
                            "Accept",
                            "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"
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
            .client(OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("x-api-key", API_KEY)
                        .header("Accept", "application/json")
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
            .baseUrl(IkiruUrl)
            .client(OkHttpClient.Builder()
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
            .baseUrl(KomikcastUrl)
            .client(OkHttpClient.Builder()
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
            .baseUrl(ComicscanUrl)
            .client(OkHttpClient.Builder()
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
            .baseUrl(IkiruUrl)
            .client(OkHttpClient.Builder()
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

    val KomryuuxSource : newSourceService by lazy {
        Retrofit.Builder()
            .baseUrl(Komryuux)
            .client(OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1"
                        )
                        .header(
                            "x-api-key", "91f30a30adcb829df7bb8d2146fef5b3104951838996284d24392a02eef83b96"
                        )
                        .header("Accept", "application/json")
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