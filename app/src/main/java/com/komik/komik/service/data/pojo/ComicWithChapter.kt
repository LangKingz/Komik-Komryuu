package com.komik.komik.service.data.pojo

import androidx.room.Embedded
import com.komik.komik.service.data.model.Comics

data class ComicWithChapter(
    @Embedded
    val comic: Comics


)
