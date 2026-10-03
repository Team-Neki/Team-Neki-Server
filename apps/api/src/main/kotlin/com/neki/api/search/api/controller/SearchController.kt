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
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
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
 * description    : 통합 검색 API. 지역·역 자동완성과, 고른 지역·역의 부스 목록·브랜드 필터
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
        summary = "지역 검색 API",
        description = """
            법정동 이름으로 지역을 검색합니다. 고른 keyword 를 부스 목록·필터 API 에 그대로 넘깁니다.

            * 접두 일치입니다. "남구" 로 "강남구" 가 나오지 않습니다
            * 부스 탭과 같은 검색어를 받습니다. 낱말이 둘 이상이면 브랜드 낱말을 빼고 찾습니다 ("강남 포토그레이" 는 "강남" 으로)
            * 이름으로 찾으면 그 구역만 나옵니다. "강남" 에 강남구는 나오지만 그 아래 역삼동은 안 나옵니다
            * "서울특별시 강남" 처럼 전체 경로로도 찾습니다. 이때는 그 아래 구역도 함께 나옵니다 (강남구, 역삼동, ...)
            * 시도는 검색 대상이 아닙니다. "서울", "서울특별시", "충북" 처럼 시도만 치면 빈 결과이고, "서울특별시 강" 처럼 공백 뒤를 이어 쳐야 그 아래 구역이 나옵니다
            * "서울 강남", "서울시 강남" 처럼 시도 줄임말로 시작해도 됩니다. 광주·전남은 "전남광주통합특별시" 로 찾습니다
            * 앞뒤 공백은 무시하고 연속 공백은 한 칸으로 봅니다
            * keyword 가 1자면 조회하지 않고 빈 결과입니다. 빈 문자열이거나 공백뿐이면 D-01
            * 결과가 없으면 빈 배열입니다. D-04 가 아닙니다
            * totalCount 는 검색어에 걸린 전체 건수입니다. 탭에 건수 배지를 다는 용도입니다
            * 정렬은 계층이 위인 것(시군구 → 읍면동 → 리)부터, 같은 계층이면 법정동코드 순입니다

            응답 keyword 는 `서울특별시 강남구` 처럼 전체 경로입니다. 같은 이름을 구분할 수 있습니다.
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
        summary = "지하철역 검색 API",
        description = """
            역명으로 지하철역을 검색합니다. 고른 keyword 를 부스 목록·필터 API 에 그대로 넘깁니다.

            * 접두 일치입니다. 1자면 조회하지 않고 빈 결과입니다. 빈 문자열이거나 공백뿐이면 D-01
            * 부스 탭과 같은 검색어를 받습니다. 낱말이 둘 이상이면 브랜드 낱말을 빼고 찾습니다 ("강남 포토그레이" 는 "강남" 으로)
            * "강남역 2호선", "강남 2호" 처럼 노선명까지 적으면 그 노선만 나옵니다
            * 한 역이 노선 수만큼 나옵니다. 노선마다 승강장 위치가 달라 주변 부스도 달라지므로 합치지 않습니다
            * 같은 이름의 다른 역이 있어 노선명까지 함께 내려줍니다
            * 위치를 안 주면 역명, 노선명 순입니다

            * latitude, longitude 를 주면 가까운 순으로 정렬되고 각 역까지의 거리(distanceKm)를 내려줍니다.
              둘 중 하나만 주거나 범위(위도 -90~90, 경도 -180~180)를 벗어나면 D-01

            응답 keyword 는 `강남역 2호선` 형태입니다. 저장된 역명에는 `역` 이 없어 서버가 붙여 줍니다.
            검색어에는 `역` 을 붙여도 되고 안 붙여도 됩니다. "강남역" 과 "강남" 은 같은 결과입니다.
            """,
    )
    @GetMapping("/completion/stations")
    fun searchStations(
        @RequestParam @NotBlank(message = "keyword는 필수값입니다.") keyword: String,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(required = false) @DecimalMin("-90") @DecimalMax("90") latitude: Double?,
        @RequestParam(required = false) @DecimalMin("-180") @DecimalMax("180") longitude: Double?,
    ): BaseResponse<SearchResponse.Completion> {
        val query: SearchQuery.SearchStations = requestConverter.toSearchStationsQuery(
            keyword = keyword,
            page = page,
            size = size,
            latitude = latitude,
            longitude = longitude,
        )

        val result: SearchResult.Completion = searchStationsUseCase.execute(query)

        val response: SearchResponse.Completion = responseConverter.toCompletionResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "부스 검색 API",
        description = """
            브랜드명, 지점명, 주소로 부스를 검색합니다. 지역·역 탭과 같은 검색어를 받습니다.

            * 검색어 전체가 이름 앞부분이면 찾습니다. "강남" 은 지점명, "포토이즘" 은 브랜드명, "포토이즘 강남" 은 둘을 이어 적은 것
            * 그 밖에도 낱말마다 브랜드명·지점명·주소 중 하나에 들어 있으면 찾습니다. 낱말 순서는 상관없습니다.
              "강남 포토그레이" 는 포토그레이 중 지점명이나 주소에 강남이 있는 부스입니다. 주소의 시도는 "서울", "서울특별시" 어느 쪽으로 적어도 됩니다
            * 정렬은 이름 앞부분 일치 → 낱말이 전부 이름에 있음 → 주소로 맞음 순이고, 그 안에서 아래 순서입니다
            * 대소문자는 구분하지 않습니다
            * 1자면 조회하지 않고 빈 결과입니다. 빈 문자열이거나 공백뿐이면 D-01
            * latitude, longitude 를 주면 가까운 순으로 정렬되고 각 부스까지의 거리(distanceKm)를 내려줍니다.
              생략하면 브랜드, 지점 이름 순입니다. 둘 중 하나만 주거나 범위(위도 -90~90, 경도 -180~180)를 벗어나면 D-01
            * 지점명이 비었거나 브랜드명과 같은 부스는 나오지 않습니다
            * 결과가 없으면 빈 배열입니다. D-04 가 아닙니다
            * 고른 대상의 부스 목록 연동은 후속 PR 에서 붙습니다

            응답 keyword 는 `포토이즘 강남1호점` 형태입니다.
            """,
    )
    @GetMapping("/completion/photo-booths")
    fun searchPhotoBoothsByKeyword(
        @RequestParam @NotBlank(message = "keyword는 필수값입니다.") keyword: String,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(required = false) @DecimalMin("-90") @DecimalMax("90") latitude: Double?,
        @RequestParam(required = false) @DecimalMin("-180") @DecimalMax("180") longitude: Double?,
    ): BaseResponse<SearchResponse.Completion> {
        val query: SearchQuery.SearchPhotoBoothsByKeyword = requestConverter.toSearchPhotoBoothsByKeywordQuery(
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
        summary = "고른 지역·역의 부스 목록 API",
        description = """
            지역·지하철역 자동완성에서 고른 keyword 에 딸린 부스 목록을 조회합니다. 지도에 한 번에 그리는 목록이라 페이징이 없습니다.
            반경을 입력받지 않습니다. 어느 역에 어느 부스가 딸리는지는 색인 시점에 1km 로 미리 계산해 둔 값입니다.

            * keyword 는 자동완성 응답의 keyword(`서울특별시 강남구`, `강남역 2호선`)를 그대로 보내거나,
              서울 자치구·지하철역과 브랜드를 섞은 검색어(`강남구 포토이즘`, `포토이즘 강남역`, `마포구`)를 보냅니다. 단어 순서는 무관합니다
            * 역만 적으면(`강남역`) 그 역의 모든 노선, 검색어에 브랜드가 있으면 그 브랜드만입니다
            * 지역을 고르면 그 아래 읍면동·리까지 포함합니다
            * keyword 가 없거나 공백뿐이면 D-01
            * 지역·역을 찾지 못하거나(브랜드만, 서울 밖 자치구, 없는 지역) 딸린 부스가 없으면 빈 배열. 둘 다 결과 없음 화면입니다
            * filterGroup 은 필수입니다. 필터를 안 걸려면 {} 를 보냅니다. brandFilter.brands 가 null 또는 [] 이면 모든 브랜드
            * userLocation 을 주면 distance 가 사용자 위치로부터의 거리(m)이고 가까운 순, 거리가 같으면 지점 이름 순입니다.
              생략하면 distance 가 null 이고 브랜드, 지점 이름 순입니다.
            * id 는 지도 부스 조회·즐겨찾기 API 와 같은 부스 ID 입니다
            """,
    )
    @PostMapping("/photo-booths")
    fun searchPhotoBooths(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Parameter(description = "자동완성 keyword 또는 자치구·역 + 브랜드 검색어", example = "서울특별시 강남구")
        @RequestParam
        @NotBlank(message = "keyword는 필수값입니다.")
        keyword: String,
        @Valid @RequestBody request: SearchRequest.GetPhotoBooths,
    ): BaseResponse<SearchResponse.GetPhotoBooths> {
        val query: SearchQuery.GetPhotoBooths = requestConverter.toGetPhotoBoothsQuery(userId, keyword, request)

        val result: SearchResult.GetPhotoBooths = searchPhotoBoothsUseCase.execute(query)

        val response: SearchResponse.GetPhotoBooths = responseConverter.toGetPhotoBoothsResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "부스 목록에서 쓸 수 있는 필터 API",
        description = """
            지금 목록에 실제로 있는 브랜드만 칩으로 띄우기 위한 API 입니다.
            keyword 와 filterGroup 은 부스 목록 API(POST /api/search/photo-booths)와 같고 userLocation 만 받지 않습니다.

            * count 는 그 범위 안에 있는 해당 브랜드의 부스 개수입니다. 부스 목록의 건수와 같습니다.
              brandFilter.brands 를 주면 그 브랜드만 집계합니다.
            * 정렬은 브랜드 전체 조회(GET /api/photo-booths/brand)와 같은 사용자별 브랜드 순서입니다.
            * 브랜드 이미지는 내려주지 않습니다. 브랜드 전체 조회의 값을 id 로 매칭해 재사용하세요.
            * keyword 가 없거나 공백뿐이면 D-01. 지역·역을 찾지 못하면 빈 배열
            """,
    )
    @PostMapping("/filter")
    fun searchFilter(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Parameter(description = "자동완성 keyword 또는 자치구·역 + 브랜드 검색어", example = "강남역 2호선")
        @RequestParam
        @NotBlank(message = "keyword는 필수값입니다.")
        keyword: String,
        @Valid @RequestBody request: SearchRequest.GetFilter,
    ): BaseResponse<SearchResponse.GetFilter> {
        val query: SearchQuery.GetFilter = requestConverter.toGetFilterQuery(userId, keyword, request)

        val result: SearchResult.GetFilter = getSearchFilterUseCase.execute(query)

        val response: SearchResponse.GetFilter = responseConverter.toGetFilterResponse(result)

        return BaseResponse(data = response)
    }
}
