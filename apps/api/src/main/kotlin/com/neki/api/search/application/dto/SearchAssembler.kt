package com.neki.api.search.application.dto

import com.neki.core.domain.vo.PageWithTotalCount
import com.neki.domain.search.models.BrandCount
import com.neki.domain.search.models.LegalDong
import com.neki.domain.search.models.PhotoBoothSearch
import com.neki.domain.search.models.SearchedBooth
import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.models.UserLocation
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * fileName       : SearchAssembler
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : 검색 결과를 자동완성, 부스 목록, 브랜드 필터 응답으로 조립한다.
 */
object SearchAssembler {

    /** 같은 이름을 구분할 수 있게 `서울특별시 강남구` 처럼 전체 경로를 내려준다. */
    fun toRegionCompletion(regions: PageWithTotalCount<LegalDong>, brandIds: List<Long>): SearchResult.Completion =
        SearchResult.Completion(
            items = regions.items.map { SearchResult.Completion.Item(keyword = it.fullName) },
            hasNext = regions.hasNext,
            totalCount = regions.totalCount,
            brandIds = brandIds,
        )

    /** 저장된 역명에는 `역` 이 없다. 화면에 보일 `강남역 2호선` 형태로 맞춰 내려준다. */
    fun toStationCompletion(
        stations: PageWithTotalCount<SubwayStation>,
        userLocation: UserLocation?,
        brandIds: List<Long>,
    ): SearchResult.Completion = SearchResult.Completion(
        items = stations.items.map {
            SearchResult.Completion.Item(
                keyword = "${it.name}역 ${it.lineName}",
                distanceKm = userLocation?.distanceKmTo(latitude = it.location.y, longitude = it.location.x),
            )
        },
        hasNext = stations.hasNext,
        totalCount = stations.totalCount,
        brandIds = brandIds,
    )

    /**
     * `포토이즘 강남1호점` 형태. 색인의 지점명은 수집한 이름에서 브랜드명 접두를 뗀 것이라(SearchNormalizer.branchName) 그대로 붙인다.
     * 부스 목록 API 의 QU 가 이 keyword 를 같은 모양(`브랜드명 지점명`)으로 색인과 비교해 그 지점으로 되돌린다.
     */
    fun toPhotoBoothCompletion(
        booths: PageWithTotalCount<PhotoBoothSearch>,
        userLocation: UserLocation?,
        brandIds: List<Long>,
    ): SearchResult.Completion = SearchResult.Completion(
        items = booths.items.map {
            SearchResult.Completion.Item(
                keyword = "${it.brandName} ${it.branchName}",
                distanceKm = userLocation?.distanceKmTo(latitude = it.location.y, longitude = it.location.x),
            )
        },
        hasNext = booths.hasNext,
        totalCount = booths.totalCount,
        brandIds = brandIds,
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

    /** 미터를 km 로 바꿔 소수 둘째 자리에서 반올림한다 (1,250m → 1.3) */
    private fun UserLocation.distanceKmTo(latitude: Double, longitude: Double): Double =
        BigDecimal(distanceTo(latitude, longitude))
            .divide(METERS_PER_KM)
            .setScale(DISTANCE_KM_SCALE, RoundingMode.HALF_UP)
            .toDouble()

    private val METERS_PER_KM: BigDecimal = BigDecimal(1_000)
    private const val DISTANCE_KM_SCALE: Int = 1
}
