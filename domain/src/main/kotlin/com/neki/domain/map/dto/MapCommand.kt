package com.neki.domain.map.dto

import com.neki.domain.map.models.PhotoBoothKey

/**
 * fileName       : MapCommand
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Map domain command
 */
object MapCommand {
    data class CollectPhotoBooth(val keyword: String, val brandCode: String)

    data class UpdateMapFavorite(val userId: Long, val locationId: Long, val favorite: Boolean)

    data class UpdatePhotoBoothFavorite(val userId: Long, val key: PhotoBoothKey, val favorite: Boolean)

    data class UpdateBrandOrder(val userId: Long, val brandIds: List<Long>)
}
