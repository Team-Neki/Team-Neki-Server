package com.neki.api.search.application.dto

import com.neki.api.search.application.SearchMockData
import com.neki.domain.search.models.UserLocation

/**
 * fileName       : SearchAssembler
 * author         : koo
 * date           : 2026. 9. 15.
 * description    : 부스를 응답 항목으로 조립한다. 부스 검색과 부스 목록이 같은 모양을 내려주므로 한곳에 둔다.
 */
object SearchAssembler {

    /**
     * 사용자 위치가 있으면 distance(m)를 채워 가까운 순, 없으면 distance 가 null 이고 브랜드·지점명 순.
     */
    fun toItems(
        booths: List<SearchMockData.PhotoBooth>,
        userLocation: UserLocation?,
    ): List<SearchResult.GetPhotoBooths.Item> {
        val items: List<SearchResult.GetPhotoBooths.Item> = booths.map {
            SearchResult.GetPhotoBooths.Item(
                id = it.id,
                brandName = it.brand.name,
                brandCode = it.brand.code,
                branchName = it.branchName,
                address = it.address,
                latitude = it.latitude,
                longitude = it.longitude,
                distance = userLocation?.distanceTo(it.latitude, it.longitude),
                favorite = it.favorite,
            )
        }

        return if (userLocation != null) {
            items.sortedBy { it.distance }
        } else {
            items.sortedWith(compareBy({ it.brandName }, { it.branchName }))
        }
    }
}
