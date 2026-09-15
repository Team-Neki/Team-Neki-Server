package com.neki.api.search.api.controller

import com.neki.api.common.api.document.RequiresSecurity
import com.neki.api.search.api.dto.SearchConverter
import com.neki.api.search.api.dto.SearchRequest
import com.neki.api.search.api.dto.SearchResponse
import com.neki.api.search.application.GetSearchFilterUseCase
import com.neki.api.search.application.SearchPhotoBoothsUseCase
import com.neki.api.search.application.dto.SearchResult
import com.neki.core.api.dto.BaseResponse
import com.neki.domain.search.dto.SearchQuery
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
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
    private val searchPhotoBoothsUseCase: SearchPhotoBoothsUseCase,
    private val getSearchFilterUseCase: GetSearchFilterUseCase,
    private val requestConverter: SearchConverter.RequestConverter,
    private val responseConverter: SearchConverter.ResponseConverter,
) {

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
