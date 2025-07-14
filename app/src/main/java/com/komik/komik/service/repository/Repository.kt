package com.komik.komik.service.repository

import com.komik.komik.service.api.model.DataItem
import com.komik.komik.service.api.model.ListItem
import com.komik.komik.service.api.model.ShinigamiHomeExplore
import com.komik.komik.service.client
import com.komik.komik.service.modelrepository.RepositorySource

class Repository(private val client:client): RepositorySource {
    override suspend fun getExplore(pageSize: Int, sortBy: String): List<DataItem> {
        val response = client.Shinigami.getMangaList(
            pageSize = pageSize,
            sortBy = sortBy
        )

        val data = response.data as List<DataItem>

        return data
    }

    override suspend fun getSourceExtension(): List<ListItem> {
        val response = client.Source.getSource()

        val data = response.list
        return data as List<ListItem>
    }

}