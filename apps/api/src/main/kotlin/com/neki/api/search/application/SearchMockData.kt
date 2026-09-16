package com.neki.api.search.application

import com.neki.domain.search.models.RegionLevel
import com.neki.domain.search.models.SearchTarget

/**
 * fileName       : SearchMockData
 * author         : koo
 * date           : 2026. 9. 15.
 * description    : 통합 검색 mock 데이터. 클라이언트 선작업용이며 실제 구현 시 제거한다.
 *                  지역과 역은 로컬에 수집해 둔 tb_legal_dong, tb_subway_station 의 실제 행이고 강남역 주변만 담았다.
 *                  지역 -> 부스는 법정동코드 enrich(BACKEND-64), 역 -> 부스는 반경 매핑이 선행돼야 한다.
 */
object SearchMockData {
    data class Brand(val id: Long, val name: String, val code: String)

    data class PhotoBooth(
        val id: Long,
        val brand: Brand,
        val branchName: String,
        val address: String,
        val latitude: Double,
        val longitude: Double,
        val favorite: Boolean,
    )

    /** 법정동. name 은 가장 아래 계층의 이름이고 검색은 이 값으로만 한다. */
    data class Region(val code: String, val level: RegionLevel, val name: String, val fullName: String)

    val PHOTOISM = Brand(id = 1, name = "포토이즘", code = "PHOTOISM")
    val LIFEFOURCUTS = Brand(id = 2, name = "인생네컷", code = "LIFEFOURCUTS")

    val photoBooths: List<PhotoBooth> = listOf(
        PhotoBooth(2560, PHOTOISM, "강남1호점", "서울 강남구 강남대로102길 16", 37.5021077, 127.0271830, favorite = true),
        PhotoBooth(2573, PHOTOISM, "강남2호점", "서울 서초구 서초대로77길 31", 37.5006179, 127.0253775, favorite = false),
        PhotoBooth(2591, PHOTOISM, "강남역점", "서울 강남구 강남대로 372", 37.4967118, 127.0289042, favorite = false),
        PhotoBooth(2604, PHOTOISM, "역삼점", "서울 강남구 테헤란로 123", 37.4998310, 127.0316420, favorite = false),
        PhotoBooth(3102, LIFEFOURCUTS, "강남역점", "서울 강남구 강남대로96길 12", 37.5002916, 127.0288671, favorite = false),
        PhotoBooth(3115, LIFEFOURCUTS, "강남2호점", "서울 서초구 강남대로 419", 37.4995520, 127.0256833, favorite = false),
    )

    /** 지역 검색 대상. 시도(SIDO)는 검색 대상이 아니라 담지 않는다. */
    val regions: List<Region> = listOf(
        Region("1168000000", RegionLevel.SIGUNGU, "강남구", "서울특별시 강남구"),
        Region("1165000000", RegionLevel.SIGUNGU, "서초구", "서울특별시 서초구"),
        Region("1168010100", RegionLevel.EUPMYEONDONG, "역삼동", "서울특별시 강남구 역삼동"),
        Region("1165010800", RegionLevel.EUPMYEONDONG, "서초동", "서울특별시 서초구 서초동"),
        Region("4817010300", RegionLevel.EUPMYEONDONG, "강남동", "경상남도 진주시 강남동"),
        Region("5279033026", RegionLevel.RI, "강남리", "전북특별자치도 고창군 무장면 강남리"),
    )

    /** 역 검색 대상. 한 역이 노선 수만큼 있고 승강장 위치가 달라 합치지 않는다. */
    val stations: List<SearchTarget.Station> = listOf(
        SearchTarget.Station("강남", "2호선"),
        SearchTarget.Station("강남", "신분당선"),
        SearchTarget.Station("강남구청", "7호선"),
        SearchTarget.Station("강남구청", "분당선"),
        SearchTarget.Station("강남대", "에버라인"),
    )

    private val allBoothIds: List<Long> = photoBooths.map { it.id }

    /** 법정동코드 -> 부스 ID. 시군구를 고르면 그 아래 읍면동까지 포함한 결과다. */
    private val regionBoothIds: Map<String, List<Long>> = mapOf(
        "1168000000" to listOf(2560, 2591, 2604, 3102), // 서울특별시 강남구
        "1165000000" to listOf(2573, 3115), // 서울특별시 서초구
        "1168010100" to listOf(2560, 2591, 2604, 3102), // 서울특별시 강남구 역삼동
        "1165010800" to listOf(2573, 3115), // 서울특별시 서초구 서초동
        "4817010300" to emptyList(), // 경상남도 진주시 강남동
        "5279033026" to emptyList(), // 전북특별자치도 고창군 무장면 강남리
    )

    /** 역(역명 x 노선) -> 부스 ID. 수집 단계에서 미리 계산한 반경 매핑을 흉내 낸다. */
    private val stationBoothIds: Map<SearchTarget.Station, List<Long>> = mapOf(
        SearchTarget.Station("강남", "2호선") to allBoothIds,
        SearchTarget.Station("강남", "신분당선") to allBoothIds,
        SearchTarget.Station("강남구청", "7호선") to emptyList(),
        SearchTarget.Station("강남구청", "분당선") to emptyList(),
        SearchTarget.Station("강남대", "에버라인") to emptyList(),
    )

    /**
     * 대상에 딸린 부스. 대상이 없으면 null, 부스가 없으면 빈 목록.
     * brandIds 가 null 이거나 비어 있으면 모든 브랜드.
     */
    fun findPhotoBooths(target: SearchTarget, brandIds: List<Long>?): List<PhotoBooth>? {
        val boothIds: List<Long> = when (target) {
            is SearchTarget.Region -> regionBoothIds[target.code]
            is SearchTarget.Station -> stationBoothIds[target]
        } ?: return null

        return photoBooths.filter { it.id in boothIds && (brandIds.isNullOrEmpty() || it.brand.id in brandIds) }
    }

    /**
     * 이름 접두 일치로 지역을 찾는다. 계층이 위인 것부터, 같은 계층이면 법정동코드 순.
     */
    fun searchRegions(keyword: String): List<Region> = regions
        .filter { it.name.startsWith(keyword) }
        .sortedWith(compareBy({ it.level.ordinal }, { it.code }))

    /**
     * 이름 접두 일치로 역을 찾는다. 역명에 `역` 접미사가 없어 `강남역` 은 `강남` 과 같은 결과다.
     */
    fun searchStations(keyword: String): List<SearchTarget.Station> {
        val name: String = keyword.removeStationSuffix()
        return stations
            .filter { it.name.startsWith(name) }
            .sortedWith(compareBy({ it.name }, { it.lineName }))
    }

    /**
     * 지점명 접두 일치로 부스를 찾는다. 브랜드명과 주소는 검색 대상이 아니다.
     */
    fun searchPhotoBooths(keyword: String): List<PhotoBooth> = photoBooths
        .filter { it.branchName.startsWith(keyword) }

    /**
     * `강남역` 처럼 뒤에 붙인 `역` 을 떼어 낸다. 한 글자짜리 `역` 검색까지 지우지는 않는다.
     */
    private fun String.removeStationSuffix(): String =
        if (length > 1 && endsWith(STATION_SUFFIX)) removeSuffix(STATION_SUFFIX) else this

    private const val STATION_SUFFIX = "역"
}
