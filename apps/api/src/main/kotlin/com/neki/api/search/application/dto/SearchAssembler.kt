package com.neki.api.search.application.dto

import com.neki.core.domain.vo.PageWithTotalCount
import com.neki.domain.search.models.BrandCount
import com.neki.domain.search.models.LegalDong
import com.neki.domain.search.models.SearchedBooth
import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.models.UserLocation

/**
 * fileName       : SearchAssembler
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : 검색 결과를 자동완성, 부스 목록, 브랜드 필터 응답으로 조립한다.
 */
object SearchAssembler {

    /** 같은 이름을 구분할 수 있게 `서울특별시 강남구` 처럼 전체 경로를 내려준다. */
    fun toRegionCompletion(regions: PageWithTotalCount<LegalDong>): SearchResult.Completion = SearchResult.Completion(
        keywords = regions.items.map { it.fullName },
        hasNext = regions.hasNext,
        totalCount = regions.totalCount,
    )

    /** 저장된 역명에는 `역` 이 없다. 화면에 보일 `강남역 2호선` 형태로 맞춰 내려준다. */
    fun toStationCompletion(stations: PageWithTotalCount<SubwayStation>): SearchResult.Completion =
        SearchResult.Completion(
            keywords = stations.items.map { "${it.name}역 ${it.lineName}" },
            hasNext = stations.hasNext,
            totalCount = stations.totalCount,
        )

    /** 순서는 SearchedBooths.ordered 가 정한 것을 그대로 쓴다 */
    fun toPhotoBooths(booths: List<SearchedBooth>, userLocation: UserLocation?): SearchResult.GetPhotoBooths =
        SearchResult.GetPhotoBooths(
            items = booths.map {
                SearchResult.GetPhotoBooths.Item(
                    id = it.mapBooth.locationId,
                    brandName = it.indexed.brandName,
                    brandCode = it.indexed.brandCode,
                    branchName = it.indexed.branchName,
                    // 지도에 있는 수집 지점은 주소가 NOT NULL 이라(V34 ck_photo_booth_location_source) 비는 일은 없다
                    address = it.indexed.address.orEmpty(),
                    latitude = it.latitude,
                    longitude = it.longitude,
                    distance = it.distanceFrom(userLocation),
                    favorite = it.mapBooth.favorite,
                )
            },
        )

    fun toBrandFilter(counts: List<BrandCount>): SearchResult.GetFilter = SearchResult.GetFilter(
        brandFilter = counts.map {
            SearchResult.GetFilter.BrandFilter(
                id = it.brandId,
                name = it.brandName,
                code = it.brandCode,
                count = it.count,
            )
        },
    )
}
