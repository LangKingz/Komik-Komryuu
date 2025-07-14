package com.komik.komik.service.database.pojo

import androidx.room.Embedded
import com.komik.komik.service.database.model.Comics

data class ComicWithChapter(
    @Embedded
    val comic: Comics


)
