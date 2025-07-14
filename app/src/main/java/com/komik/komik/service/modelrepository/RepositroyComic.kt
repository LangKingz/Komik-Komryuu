package com.komik.komik.service.modelrepository

import com.komik.komik.service.database.model.Comic
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.parser.model.getListUpdate

interface RepositroyComic {
    suspend fun getComic(type:sourceType,url:String) : List<getListUpdate>


}

interface RepositoryDetail {
    suspend fun getDetail(type:sourceType,url:String): Comic?
}