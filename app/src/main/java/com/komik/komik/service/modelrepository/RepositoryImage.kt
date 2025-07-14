package com.komik.komik.service.modelrepository

import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.parser.model.getImages


interface RepositoryImage {
    suspend fun getImage(url: String,type:sourceType): List<getImages>
}