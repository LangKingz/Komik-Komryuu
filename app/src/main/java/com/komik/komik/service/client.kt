package com.komik.komik.service

import com.komik.komik.service.api.Scrape
import com.komik.komik.service.api.ScrapeComicscan
import com.komik.komik.service.api.ScrapeIkiru
import com.komik.komik.service.api.ScrapeKomikcast
import com.komik.komik.service.api.ShinigamiService
import com.komik.komik.service.api.SourceService
import com.komik.komik.service.utils.generateFakeCookie
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object client {

    private const val URLSHINIGAMIAPI = "https://api.shngm.io/"
    private const val URLSOURCE = "https://www.komryuu.my.id/"
    private const val IkiruUrl = "https://id.ikiru.wtf/"
    private const val KomikcastUrl = "https://komikcast.li/"
    private const val ComicscanUrl = "https://lc3.cosmicscans.asia/"


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
                        .build()
                    chain.proceed(request)
                }
                .build()
            )
            .build()
            .create(ShinigamiService::class.java)
    }

    val Source: SourceService by lazy {
        Retrofit.Builder()
            .baseUrl(URLSOURCE)
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
                            generateFakeCookie()
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


}