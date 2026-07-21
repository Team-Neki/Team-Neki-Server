package com.neki.media.api.converter

import com.neki.media.application.dto.MediaQuery
import org.springframework.stereotype.Component

/**
 * fileName       : FileCommandConverter
 * author         : koo
 * date           : 2026. 1. 21.
 * description    : File api layer query 변환을 위한 converter
 */
@Component
class FileCommandConverter {

    fun toGetImageByKeyQuery(objectKey: String): MediaQuery.GetImageByKey =
        MediaQuery.GetImageByKey(objectKey = objectKey)
}
