package com.neki.api.map.api.dto

import com.neki.api.map.application.dto.MapResult
import com.neki.core.code.ResultCode
import com.neki.core.domain.vo.BranchName
import com.neki.core.exception.BusinessException
import com.neki.domain.map.dto.MapCommand
import com.neki.domain.map.models.PhotoBoothKey
import org.springframework.stereotype.Component

/**
 * fileName       : MapV2Converter
 * author         : darren
 * date           : 2026. 10. 13.
 * description    : 지도 V2 api layer converter. 조회 요청은 V1 과 같아 [MapConverter.RequestConverter] 를 쓴다
 */
object MapV2Converter {
    @Component
    class RequestConverter {
        /** 모양이 맞지 않는 부스 id 는 D-01 */
        fun toUpdatePhotoBoothFavoriteCommand(
            userId: Long,
            photoBoothId: String,
            request: MapRequest.UpdateMapFavorite,
        ): MapCommand.UpdatePhotoBoothFavorite = MapCommand.UpdatePhotoBoothFavorite(
            userId = userId,
            key = PhotoBoothKey.fromBoothId(photoBoothId) ?: throw BusinessException(ResultCode.INVALID_PARAMETER),
            favorite = request.favorite!!,
        )
    }

    @Component
    class ResponseConverter {
        fun toGetPolygonPhotoBoothsResponse(
            result: MapResult.GetPolygonPhotoBooths,
        ): MapV2Response.GetPolygonPhotoBooths {
            val items: List<MapV2Response.GetPolygonPhotoBooths.Item> = result.photoBooths.map {
                MapV2Response.GetPolygonPhotoBooths.Item(
                    id = it.key.toBoothId(),
                    brandName = it.brandName,
                    branchName = BranchName.of(it.brandName, it.branchName),
                    address = it.address.orEmpty(),
                    longitude = it.location.x,
                    latitude = it.location.y,
                    favorite = it.key in result.favoriteKeys,
                )
            }
            return MapV2Response.GetPolygonPhotoBooths(items = items)
        }

        fun toGetPointPhotoBoothsResponse(result: MapResult.GetPointPhotoBooths): MapV2Response.GetPointPhotoBooths {
            val items: List<MapV2Response.GetPointPhotoBooths.Item> = result.photoBooths.map {
                MapV2Response.GetPointPhotoBooths.Item(
                    id = it.key.toBoothId(),
                    brandName = it.brandName,
                    branchName = BranchName.of(it.brandName, it.branchName),
                    address = it.address.orEmpty(),
                    longitude = it.location.x,
                    latitude = it.location.y,
                    distance = it.distance,
                    favorite = it.key in result.favoriteKeys,
                )
            }
            return MapV2Response.GetPointPhotoBooths(items = items)
        }

        fun toGetFavoritePhotoBoothsResponse(
            result: MapResult.GetFavoritePhotoBooths,
        ): MapV2Response.GetFavoritePhotoBooths {
            val items: List<MapV2Response.GetFavoritePhotoBooths.Item> = result.photoBooths.map {
                MapV2Response.GetFavoritePhotoBooths.Item(
                    id = it.key.toBoothId(),
                    brandName = it.brandName,
                    branchName = BranchName.of(it.brandName, it.branchName),
                    address = it.address.orEmpty(),
                    longitude = it.location.x,
                    latitude = it.location.y,
                )
            }
            return MapV2Response.GetFavoritePhotoBooths(items = items)
        }
    }
}
