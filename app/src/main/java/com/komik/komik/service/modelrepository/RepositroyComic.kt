package com.komik.komik.service.modelrepository

import com.komik.komik.service.data.model.Comic
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.parser.model.ParsedData
import com.komik.komik.service.parser.model.getListUpdate

interface RepositroyComic {
//    suspend fun getComic(type: sourceType, url: String, pages: Int = 10): List<getListUpdate>
    suspend fun getComicPaged(type: sourceType, url: String, limit: Int = 24, offset: Int = 0): List<getListUpdate>
    suspend fun getSearch(type: sourceType, query: String, url: String): List<getListUpdate>
}

interface RepositoryDetail {
    suspend fun getDetail(type:sourceType,url:String): ParsedData?
}