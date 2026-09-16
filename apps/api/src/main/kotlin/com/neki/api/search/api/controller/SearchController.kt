package com.neki.api.search.api.controller

import com.neki.api.common.api.document.RequiresSecurity
import com.neki.api.search.api.dto.SearchConverter
import com.neki.api.search.api.dto.SearchRequest
import com.neki.api.search.api.dto.SearchResponse
import com.neki.api.search.application.GetSearchFilterUseCase
import com.neki.api.search.application.SearchPhotoBoothsByKeywordUseCase
import com.neki.api.search.application.SearchPhotoBoothsUseCase
import com.neki.api.search.application.SearchRegionsUseCase
import com.neki.api.search.application.SearchStationsUseCase
import com.neki.api.search.application.dto.SearchResult
import com.neki.core.api.dto.BaseResponse
import com.neki.domain.search.dto.SearchQuery
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : SearchController
 * author         : koo
 * date           : 2026. 9. 15. 오후 5:27
 * description    : 통합 검색 API. 현재는 클라이언트 선작업용 mock 응답이다.
 */
@RequiresSecurity
@Tag(name = "search", description = "통합 검색 API")
@RestController
@RequestMapping("/api/search")
class SearchController(
    private val searchRegionsUseCase: SearchRegionsUseCase,
    private val searchStationsUseCase: SearchStationsUseCase,
    private val searchPhotoBoothsByKeywordUseCase: SearchPhotoBoothsByKeywordUseCase,
    private val searchPhotoBoothsUseCase: SearchPhotoBoothsUseCase,
    private val getSearchFilterUseCase: GetSearchFilterUseCase,
    private val requestConverter: SearchConverter.RequestConverter,
    private val responseConverter: SearchConverter.ResponseConverter,
) {

    @Operation(
        summary = "지역 검색 API (mock)",
        description = """
            법정동 이름으로 지역을 검색합니다. 고른 결과의 code 를 부스 목록 API 에 넘깁니다.

            * 접두 일치입니다. "남구" 로 "강남구" 가 나오지 않습니다
            * 검색어와 직접 매칭되는 구역만 내려갑니다. "강남" 에 강남구는 나오지만 그 아래 방배동 같은 하위 구역은 안 나옵니다
            * 시도(SIDO)는 검색 대상이 아닙니다. "서울" 만으로는 검색되지 않습니다
            * keyword 는 1자도 됩니다. 빈 문자열이거나 공백뿐이면 D-01
            * 결과가 없으면 빈 배열입니다. D-04 가 아닙니다
            * totalCount 는 검색어에 걸린 전체 건수입니다. 탭에 건수 배지를 다는 용도입니다

            현재는 mock 응답입니다. 강남역 주변과 검색 예시에 필요한 법정동만 있습니다.
            """,
    )
    @GetMapping("/regions")
    fun searchRegions(
        @RequestParam @NotBlank(message = "keyword는 필수값입니다.") keyword: String,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): BaseResponse<SearchResponse.SearchRegions> {
        val query: SearchQuery.SearchRegions = requestConverter.toSearchRegionsQuery(keyword, page, size)

        val result: SearchResult.SearchRegions = searchRegionsUseCase.execute(query)

        val response: SearchResponse.SearchRegions = responseConverter.toSearchRegionsResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "지하철역 검색 API (mock)",
        description = """
            역명으로 지하철역을 검색합니다. 고른 결과의 name 과 lineName 을 부스 목록 API 에 그대로 넘깁니다.

            * 접두 일치이고 검색 규칙은 지역 검색과 같습니다
            * 한 역이 노선 수만큼 나옵니다. 노선마다 승강장 위치가 달라 주변 부스도 달라지므로 합치지 않습니다
            * 역명에 `역` 을 붙이지 않습니다. "강남역" 으로 검색해도 "강남" 과 같은 결과입니다
            * 같은 이름의 다른 역이 있어 lineName 으로 구분합니다

            현재는 mock 응답입니다. 강남(2호선·신분당선), 강남구청(7호선·분당선), 강남대(에버라인)만 있습니다.
            """,
    )
    @GetMapping("/stations")
    fun searchStations(
        @RequestParam @NotBlank(message = "keyword는 필수값입니다.") keyword: String,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): BaseResponse<SearchResponse.SearchStations> {
        val query: SearchQuery.SearchStations = requestConverter.toSearchStationsQuery(keyword, page, size)

        val result: SearchResult.SearchStations = searchStationsUseCase.execute(query)

        val response: SearchResponse.SearchStations = responseConverter.toSearchStationsResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "부스 검색 API (mock)",
        description = """
            지점명으로 부스를 검색합니다. 브랜드명과 주소는 검색 대상이 아닙니다.
            지도에 필요한 값이 응답에 다 들어 있어 고른 뒤 추가 호출이 없습니다.

            * 접두 일치이고 검색 규칙은 지역 검색과 같습니다
            * latitude, longitude 를 주면 distance 가 그 위치로부터의 거리(m)이고 가까운 순으로 정렬됩니다.
              생략하면 distance 가 null 이고 브랜드, 지점 이름 순입니다. 둘 중 하나만 주면 D-01
            * 고른 대상의 부스 목록은 같은 경로의 POST /api/search/photo-booths 입니다. 이 API 는 검색어로 찾는 쪽입니다

            현재는 mock 응답입니다.
            """,
    )
    @GetMapping("/photo-booths")
    fun searchPhotoBoothsByKeyword(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @RequestParam @NotBlank(message = "keyword는 필수값입니다.") keyword: String,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(required = false) latitude: Double?,
        @RequestParam(required = false) longitude: Double?,
    ): BaseResponse<SearchResponse.SearchPhotoBooths> {
        val query: SearchQuery.SearchPhotoBoothsByKeyword = requestConverter.toSearchPhotoBoothsByKeywordQuery(
            userId = userId,
            keyword = keyword,
            page = page,
            size = size,
            latitude = latitude,
            longitude = longitude,
        )

        val result: SearchResult.SearchPhotoBooths = searchPhotoBoothsByKeywordUseCase.execute(query)

        val response: SearchResponse.SearchPhotoBooths = responseConverter.toSearchPhotoBoothsResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "고른 지역·역의 부스 목록 API (mock)",
        description = """
            지역 검색 또는 역 검색에서 고른 대상에 딸린 부스 목록을 조회합니다. 지도에 한 번에 그리는 목록이라 페이징이 없습니다.
            반경을 입력받지 않습니다. 어느 역에 어느 부스가 딸리는지는 수집 단계에서 미리 계산해 둔 값입니다.

            * regionFilter 와 stationFilter 중 하나만 보냅니다. 둘 다 없거나 둘 다 있으면 D-01
            * 없는 지역·역이면 D-04, 부스가 없으면 빈 배열
            * userLocation 을 주면 distance 가 사용자 위치로부터의 거리(m)이고 가까운 순으로 정렬됩니다.
              생략하면 distance 가 null 이고 브랜드, 지점 이름 순입니다.
            * brandFilter.brandIds 가 null 또는 [] 이면 모든 브랜드

            현재는 mock 응답입니다. 지역 코드 1168000000(강남구), 1165000000(서초구), 4817010300, 5279033026 과
            역 강남(2호선, 신분당선), 강남구청(7호선, 분당선), 강남대(에버라인)만 존재합니다.
            """,
    )
    @PostMapping("/photo-booths")
    fun searchPhotoBooths(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: SearchRequest.FilterGroup,
    ): BaseResponse<SearchResponse.GetPhotoBooths> {
        val query: SearchQuery.GetPhotoBooths = requestConverter.toGetPhotoBoothsQuery(userId, request)

        val result: SearchResult.GetPhotoBooths = searchPhotoBoothsUseCase.execute(query)

        val response: SearchResponse.GetPhotoBooths = responseConverter.toGetPhotoBoothsResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "부스 목록에서 쓸 수 있는 필터 API (mock)",
        description = """
            지금 목록에 실제로 있는 브랜드만 칩으로 띄우기 위한 API 입니다.
            요청 body 는 부스 목록 API(POST /api/search/photo-booths)와 완전히 같습니다. userLocation 은 무시됩니다.

            * count 는 그 범위 안에 있는 해당 브랜드의 부스 개수입니다. brandFilter.brandIds 를 주면 그 브랜드만 집계합니다.
            * 브랜드 이미지는 내려주지 않습니다. 브랜드 전체 조회(GET /api/photo-booths/brand)의 값을 id 로 매칭해 재사용하세요.
            * 없는 지역·역이면 D-04

            현재는 mock 응답입니다. 정렬은 브랜드 ID 순이며, 실제 구현은 사용자별 브랜드 정렬 순서를 따릅니다.
            """,
    )
    @PostMapping("/filter")
    fun searchFilter(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: SearchRequest.FilterGroup,
    ): BaseResponse<SearchResponse.GetFilter> {
        val query: SearchQuery.GetFilter = requestConverter.toGetFilterQuery(userId, request)

        val result: SearchResult.GetFilter = getSearchFilterUseCase.execute(query)

        val response: SearchResponse.GetFilter = responseConverter.toGetFilterResponse(result)

        return BaseResponse(data = response)
    }
}
