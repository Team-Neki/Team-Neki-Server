package com.neki.api.search.application

import com.neki.domain.search.models.RegionLevel
import com.neki.domain.search.models.Station

/**
 * fileName       : SearchMockData
 * author         : koo
 * date           : 2026. 9. 15.
 * description    : 통합 검색 mock 데이터. 클라이언트 선작업용이며 실제 구현 시 제거한다.
 *                  부스 목록·필터는 keyword, filterGroup, userLocation 과 무관하게 항상 이 부스 6개를 내려준다.
 *                  지역과 역은 로컬에 수집해 둔 tb_legal_dong, tb_subway_station 의 실제 행이고 강남역 주변만 담았다.
 *                  실제 구현은 지역 -> 부스 법정동코드 enrich(BACKEND-64), 역 -> 부스 반경 매핑이 선행돼야 한다.
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
        val distance: Int,
        val favorite: Boolean,
    )

    /** 법정동. name 은 가장 아래 계층의 이름이고 검색은 이 값으로만 한다. */
    data class Region(val code: String, val level: RegionLevel, val name: String, val fullName: String)

    val PHOTOISM = Brand(id = 1, name = "포토이즘", code = "PHOTOISM")
    val LIFEFOURCUTS = Brand(id = 2, name = "인생네컷", code = "LIFEFOURCUTS")

    /** distance 는 강남역(37.4979, 127.0276) 기준 haversine 거리(m). 가까운 순으로 고정. */
    val photoBooths: List<PhotoBooth> = listOf(
        PhotoBooth(2591, PHOTOISM, "강남역점", "서울 강남구 강남대로 372", 37.4967118, 127.0289042, 175, favorite = false),
        PhotoBooth(3115, LIFEFOURCUTS, "강남2호점", "서울 서초구 강남대로 419", 37.4995520, 127.0256833, 250, favorite = false),
        PhotoBooth(3102, LIFEFOURCUTS, "강남역점", "서울 강남구 강남대로96길 12", 37.5002916, 127.0288671, 288, favorite = false),
        PhotoBooth(2573, PHOTOISM, "강남2호점", "서울 서초구 서초대로77길 31", 37.5006179, 127.0253775, 360, favorite = false),
        PhotoBooth(2604, PHOTOISM, "역삼점", "서울 강남구 테헤란로 123", 37.4998310, 127.0316420, 416, favorite = false),
        PhotoBooth(2560, PHOTOISM, "강남1호점", "서울 강남구 강남대로102길 16", 37.5021077, 127.0271830, 469, favorite = true),
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
    val stations: List<Station> = listOf(
        Station("강남", "2호선"),
        Station("강남", "신분당선"),
        Station("강남구청", "7호선"),
        Station("강남구청", "분당선"),
        Station("강남대", "에버라인"),
    )

    /**
     * 이름 접두 일치로 지역을 찾는다. 계층이 위인 것부터, 같은 계층이면 법정동코드 순.
     */
    fun searchRegions(keyword: String): List<Region> = regions
        .filter { it.name.startsWith(keyword) }
        .sortedWith(compareBy({ it.level.ordinal }, { it.code }))

    /**
     * 이름 접두 일치로 역을 찾는다. 역명에 `역` 접미사가 없어 `강남역` 은 `강남` 과 같은 결과다.
     */
    fun searchStations(keyword: String): List<Station> {
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
