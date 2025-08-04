package com.komik.komik.service.modelrepository

import com.komik.komik.service.api.model.DataItem
import com.komik.komik.service.api.model.DataItems
import com.komik.komik.service.api.model.ListItem
import com.komik.komik.service.api.model.ShinigamiHomeExplore

interface RepositorySource {
    suspend fun getExplore(pageSize:Int,sortBy:String) : List<DataItem>
    suspend fun getSourceExtension(): List<DataItems>
}