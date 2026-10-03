package com.neki.domain.search.models

/**
 * fileName       : SearchedBooths
 * author         : koo
 * date           : 2026. 10. 2.
 * description    : 부스 목록과 브랜드 필터가 함께 쓰는 검색 결과 집합. 검색 색인 행을 지도 부스와 짝지은 것이다.
 *   지도에 없거나 관리자가 숨긴 지점은 들어오지 않으므로, 브랜드 칩의 개수와 목록 건수가 늘 맞는다.
 */
class SearchedBooths private constructor(private val booths: List<SearchedBooth>) {

    val size: Int
        get() = booths.size

    /**
     * 검색 정책 12장. 사용자 위치가 있으면 가까운 순이고 거리가 같으면 지점명 순, 없으면 브랜드·지점명 순.
     * 클라이언트는 이 순서를 그대로 쓰고 재정렬하지 않는다.
     */
    fun ordered(userLocation: UserLocation?): List<SearchedBooth> = if (userLocation != null) {
        booths.sortedWith(compareBy({ it.distanceFrom(userLocation) }, { it.indexed.branchName }))
    } else {
        booths.sortedWith(compareBy({ it.indexed.brandName }, { it.indexed.branchName }))
    }

    /**
     * 브랜드별 부스 수. 정렬은 사용자별 브랜드 순서(orderedBrandIds)이고, 순서에 없는 브랜드는 뒤에 id 순.
     */
    fun countByBrand(orderedBrandIds: List<Long>): List<BrandCount> {
        val sortOrder: Map<Long, Int> = orderedBrandIds.withIndex().associate { (index, brandId) -> brandId to index }

        return booths
            .groupBy { it.indexed.brandId }
            .map { (brandId, brandBooths) ->
                val first: PhotoBoothSearch = brandBooths.first().indexed
                BrandCount(
                    brandId = brandId,
                    brandName = first.brandName,
                    brandCode = first.brandCode,
                    count = brandBooths.size,
                )
            }
            .sortedWith(compareBy({ sortOrder[it.brandId] ?: Int.MAX_VALUE }, { it.brandId }))
    }

    companion object {
        /** 원천 키 (platform, idx) 가 같은 색인 행과 지도 부스를 짝짓는다. 둘 다 원천 키가 유일해 1:1 이다 */
        fun of(indexedBooths: List<PhotoBoothSearch>, mapBooths: List<MapBooth>): SearchedBooths {
            val bySource: Map<Pair<String, String>, MapBooth> = mapBooths.associateBy { it.platform to it.idx }

            return SearchedBooths(
                indexedBooths.mapNotNull { booth ->
                    bySource[booth.platform to booth.idx]?.let { SearchedBooth(indexed = booth, mapBooth = it) }
                },
            )
        }
    }
}

/**
 * 검색 결과 부스 하나. 부스 정보는 색인 행(색인 시점 스냅샷), id 와 즐겨찾기는 지도 부스 값이다.
 */
class SearchedBooth(val indexed: PhotoBoothSearch, val mapBooth: MapBooth) {

    val latitude: Double
        get() = indexed.location.y

    val longitude: Double
        get() = indexed.location.x

    /** 사용자 위치에서 이 부스까지의 거리(m). 위치가 없으면 null */
    fun distanceFrom(userLocation: UserLocation?): Int? = userLocation?.distanceTo(latitude, longitude)
}

/** 브랜드별 부스 수. 브랜드 이름과 코드는 색인 시점 스냅샷이다 */
data class BrandCount(val brandId: Long, val brandName: String, val brandCode: String, val count: Int)
