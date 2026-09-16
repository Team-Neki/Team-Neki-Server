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
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.security.core.annotation.AuthenticationPrincipal
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
    private val searchPhotoBoothsUseCase: SearchPhotoBoothsUseCase,
    private val getSearchFilterUseCase: GetSearchFilterUseCase,
    private val requestConverter: SearchConverter.RequestConverter,
    private val responseConverter: SearchConverter.ResponseConverter,
) {

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
