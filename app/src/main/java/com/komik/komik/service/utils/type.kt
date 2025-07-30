package com.komik.komik.service.utils

import com.komik.komik.service.enumclass.sourceType

object typeSource {
    fun getSourceType(type: String): sourceType {
        return when(type){
            "ikiru" -> sourceType.ikiru
            "comicscan" -> sourceType.comicscan
            "shinigami" -> sourceType.shinigami
            "komikcast" -> sourceType.komikcast
            "kiryuu" -> sourceType.kiryuu
            "komiku" -> sourceType.komiku
            "manhwaindo" -> sourceType.manhwaindo
            "dd" -> sourceType.doujindesu
            "batoto-all" -> sourceType.batoto_all
            else -> sourceType.ikiru
        }
    }
}
