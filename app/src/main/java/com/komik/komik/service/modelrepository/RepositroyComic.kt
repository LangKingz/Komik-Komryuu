package com.komik.komik.service.modelrepository

import com.komik.komik.service.data.model.Comic
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.parser.model.ParsedData
import com.komik.komik.service.parser.model.getListUpdate

interface RepositroyComic {
    suspend fun getComic(type:sourceType,url:String) : List<getListUpdate>
    suspend fun getSearch(type: sourceType,query:String="") : List<getListUpdate>


}

interface RepositoryDetail {
    suspend fun getDetail(type:sourceType,url:String): ParsedData?
}