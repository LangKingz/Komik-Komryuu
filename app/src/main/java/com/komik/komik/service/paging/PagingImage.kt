package com.komik.komik.service.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.parser.model.getImages
import com.komik.komik.service.repository.Image

class PagingImage(
    private val repository: Image,
    private val url: String,
    private val type: sourceType
) : PagingSource<Int, getImages>() {
    override fun getRefreshKey(state: PagingState<Int, getImages>): Int? {
        return state.anchorPosition?.let { position ->
            state.closestPageToPosition(position)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(position)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, getImages> {
        val currentPage = params.key ?: 1

        return try {
            val images = repository.getImage(url, type)

            LoadResult.Page(
                data = images,
                prevKey = if (currentPage == 1) null else currentPage - 1,
                nextKey = if (images.isEmpty()) null else currentPage + 1
            )
        } catch (e: Exception) {

            LoadResult.Error(e)
        }
    }

}