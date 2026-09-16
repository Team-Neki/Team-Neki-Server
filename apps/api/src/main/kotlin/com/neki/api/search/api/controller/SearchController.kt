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
import io.swagger.v3.oas.annotations.Parameter
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
            법정동 이름으로 지역을 검색합니다. 고른 keyword 를 부스 목록 API 에 넘깁니다.

            * 접두 일치입니다. "남구" 로 "강남구" 가 나오지 않습니다
            * 검색어와 직접 매칭되는 구역만 내려갑니다. "강남" 에 강남구는 나오지만 그 아래 방배동 같은 하위 구역은 안 나옵니다
            * 시도는 검색 대상이 아닙니다. "서울" 만으로는 검색되지 않습니다
            * keyword 는 1자도 됩니다. 빈 문자열이거나 공백뿐이면 D-01
            * 결과가 없으면 빈 배열입니다. D-04 가 아닙니다
            * totalCount 는 검색어에 걸린 전체 건수입니다. 탭에 건수 배지를 다는 용도입니다

            응답 keyword 는 `서울특별시 강남구` 처럼 전체 경로입니다. 같은 이름을 구분할 수 있습니다.
            현재는 mock 응답입니다. 강남역 주변과 검색 예시에 필요한 법정동만 있습니다.
            """,
    )
    @GetMapping("/completion/regions")
    fun searchRegions(
        @RequestParam @NotBlank(message = "keyword는 필수값입니다.") keyword: String,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): BaseResponse<SearchResponse.Completion> {
        val query: SearchQuery.SearchRegions = requestConverter.toSearchRegionsQuery(keyword, page, size)

        val result: SearchResult.Completion = searchRegionsUseCase.execute(query)

        val response: SearchResponse.Completion = responseConverter.toCompletionResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "지하철역 검색 API (mock)",
        description = """
            역명으로 지하철역을 검색합니다. 고른 keyword 를 부스 목록 API 에 넘깁니다.

            * 접두 일치이고 검색 규칙은 지역 검색과 같습니다
            * 한 역이 노선 수만큼 나옵니다. 노선마다 승강장 위치가 달라 주변 부스도 달라지므로 합치지 않습니다
            * 같은 이름의 다른 역이 있어 노선명까지 함께 내려줍니다

            응답 keyword 는 `강남역 2호선` 형태입니다. 저장된 역명에는 `역` 이 없어 서버가 붙여 줍니다.
            검색어에는 `역` 을 붙여도 되고 안 붙여도 됩니다. "강남역" 과 "강남" 은 같은 결과입니다.

            현재는 mock 응답입니다. 강남(2호선·신분당선), 강남구청(7호선·분당선), 강남대(에버라인)만 있습니다.
            """,
    )
    @GetMapping("/completion/stations")
    fun searchStations(
        @RequestParam @NotBlank(message = "keyword는 필수값입니다.") keyword: String,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): BaseResponse<SearchResponse.Completion> {
        val query: SearchQuery.SearchStations = requestConverter.toSearchStationsQuery(keyword, page, size)

        val result: SearchResult.Completion = searchStationsUseCase.execute(query)

        val response: SearchResponse.Completion = responseConverter.toCompletionResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "부스 검색 API (mock)",
        description = """
            지점명으로 부스를 검색합니다. 브랜드명과 주소는 검색 대상이 아닙니다.

            * 접두 일치이고 검색 규칙은 지역 검색과 같습니다
            * latitude, longitude 를 주면 가까운 순으로 정렬됩니다. 생략하면 브랜드, 지점 이름 순입니다.
              둘 중 하나만 주면 D-01
            * 고른 대상의 부스 목록은 POST /api/search/photo-booths 입니다

            응답 keyword 는 `포토이즘 강남1호점` 형태입니다.
            현재는 mock 응답입니다.
            """,
    )
    @GetMapping("/completion/photo-booths")
    fun searchPhotoBoothsByKeyword(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @RequestParam @NotBlank(message = "keyword는 필수값입니다.") keyword: String,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(required = false) latitude: Double?,
        @RequestParam(required = false) longitude: Double?,
    ): BaseResponse<SearchResponse.Completion> {
        val query: SearchQuery.SearchPhotoBoothsByKeyword = requestConverter.toSearchPhotoBoothsByKeywordQuery(
            userId = userId,
            keyword = keyword,
            page = page,
            size = size,
            latitude = latitude,
            longitude = longitude,
        )

        val result: SearchResult.Completion = searchPhotoBoothsByKeywordUseCase.execute(query)

        val response: SearchResponse.Completion = responseConverter.toCompletionResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "검색어에 맞는 부스 목록 API (mock)",
        description = """
            검색어(keyword)에 맞는 부스 목록을 조회합니다. 지도에 한 번에 그리는 목록이라 페이징이 없습니다.
            반경을 입력받지 않습니다.

            * keyword 는 쿼리 파라미터로 보내며 필수입니다. 없거나 공백만 있으면 D-01
            * filterGroup 은 필수입니다. 필터를 안 걸려면 {} 를 보냅니다
            * 실제 구현: 맞는 부스가 없으면 빈 배열. userLocation 을 주면 distance 가 사용자 위치로부터의 거리(m)이고 가까운 순,
              생략하면 distance 가 null 이고 브랜드·지점 이름 순. brandFilter.brands 가 null 또는 [] 이면 모든 브랜드

            현재는 mock 응답입니다. keyword, filterGroup, userLocation 과 무관하게 항상 같은 부스 6개를
            강남역(37.4979, 127.0276) 기준 가까운 순으로 내려줍니다.
            """,
    )
    @PostMapping("/photo-booths")
    fun searchPhotoBooths(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Parameter(description = "검색어. 지역·역·부스 이름", example = "강남")
        @RequestParam
        @NotBlank keyword: String,
        @Valid @RequestBody request: SearchRequest.GetPhotoBooths,
    ): BaseResponse<SearchResponse.GetPhotoBooths> {
        val query: SearchQuery.GetPhotoBooths = requestConverter.toGetPhotoBoothsQuery(userId, keyword, request)

        val result: SearchResult.GetPhotoBooths = searchPhotoBoothsUseCase.execute(query)

        val response: SearchResponse.GetPhotoBooths = responseConverter.toGetPhotoBoothsResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "부스 목록에서 쓸 수 있는 필터 API (mock)",
        description = """
            지금 목록에 실제로 있는 브랜드만 칩으로 띄우기 위한 API 입니다.
            keyword 와 filterGroup 은 부스 목록 API(POST /api/search/photo-booths)와 같고 userLocation 만 받지 않습니다.

            * keyword 는 쿼리 파라미터로 보내며 필수입니다. 없거나 공백만 있으면 D-01
            * count 는 검색 결과 안에 있는 해당 브랜드의 부스 개수입니다. 실제 구현은 brandFilter.brands 를 주면 그 브랜드만 집계합니다.
            * 브랜드 이미지는 내려주지 않습니다. 브랜드 전체 조회(GET /api/photo-booths/brand)의 값을 id 로 매칭해 재사용하세요.

            현재는 mock 응답입니다. keyword, filterGroup 과 무관하게 항상 포토이즘 4개, 인생네컷 2개를 브랜드 ID 순으로 내려줍니다.
            실제 구현은 사용자별 브랜드 정렬 순서를 따릅니다.
            """,
    )
    @PostMapping("/filter")
    fun searchFilter(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Parameter(description = "검색어. 지역·역·부스 이름", example = "강남")
        @RequestParam
        @NotBlank keyword: String,
        @Valid @RequestBody request: SearchRequest.GetFilter,
    ): BaseResponse<SearchResponse.GetFilter> {
        val query: SearchQuery.GetFilter = requestConverter.toGetFilterQuery(userId, keyword, request)

        val result: SearchResult.GetFilter = getSearchFilterUseCase.execute(query)

        val response: SearchResponse.GetFilter = responseConverter.toGetFilterResponse(result)

        return BaseResponse(data = response)
    }
}
