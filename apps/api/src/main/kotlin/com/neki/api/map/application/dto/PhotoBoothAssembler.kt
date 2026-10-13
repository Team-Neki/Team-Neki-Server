package com.neki.api.map.application.dto

import com.neki.domain.map.models.PhotoBoothKey

/**
 * fileName       : PhotoBoothAssembler
 * author         : darren
 * date           : 2026. 10. 13.
 * description    : 포토부스(enriched)를 다른 도메인에 넘길 결과로 조립한다.
 */
object PhotoBoothAssembler {

    fun toPhotoBoothsByKeys(
        existingKeys: Set<PhotoBoothKey>,
        favoriteKeys: Set<PhotoBoothKey>,
    ): MapResult.GetPhotoBoothsByKeys = MapResult.GetPhotoBoothsByKeys(
        photoBooths = existingKeys.map {
            MapResult.GetPhotoBoothsByKeys.PhotoBooth(key = it, favorite = it in favoriteKeys)
        },
    )
}
