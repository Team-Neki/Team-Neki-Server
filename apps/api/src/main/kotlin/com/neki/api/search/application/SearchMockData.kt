package com.neki.api.search.application

/**
 * fileName       : SearchMockData
 * author         : koo
 * date           : 2026. 9. 15.
 * description    : 통합 검색 mock 데이터. 클라이언트 선작업용이며 실제 구현 시 제거한다.
 *                  keyword, filterGroup, userLocation 과 무관하게 항상 이 부스 6개를 내려준다.
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
}
