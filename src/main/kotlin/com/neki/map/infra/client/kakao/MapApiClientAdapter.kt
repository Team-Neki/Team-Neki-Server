package com.neki.map.infra.client.kakao

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.map.application.port.MapApiClientPort
import com.neki.map.application.port.dto.MapContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestClient

/**
 * fileName       : MapApiClient
 * author         : darren
 * date           : 2026. 01. 13.
 * description    : Kakao Local API 클라이언트
 */
class MapApiClientAdapter(private val apiKey: String, private val restClient: RestClient) : MapApiClientPort {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * Kakao Local API 키워드 검색
     * @param query 검색 키워드
     * @param page 페이지 번호 (1~45)
     * @param size 한 페이지에 보여질 문서의 개수 (1~15)
     * @param rect 사각형 범위 (x1,y1,x2,y2 - 좌하단 경도,위도,우상단 경도,위도)
     * @return KakaoLocalSearchResponse
     */
    override fun searchByKeyword(query: String, page: Int, size: Int, rect: String?): MapContract.LocalSearchResult {
        log.info("Kakao API Request - query: {}, page: {}, size: {}, rect: {}", query, page, size, rect)

        val response: KakaoLocalSearchPayload = restClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .scheme("https")
                    .host("dapi.kakao.com")
                    .path("/v2/local/search/keyword.json")
                    .queryParam("query", query)
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .apply {
                        if (rect != null) {
                            queryParam("rect", rect)
                        }
                    }
                    .build()
            }
            .header(HttpHeaders.AUTHORIZATION, "KakaoAK $apiKey")
            .retrieve()
            .body(KakaoLocalSearchPayload::class.java)
            ?: throw BusinessException(ResultCode.ERROR)

        return MapContract.LocalSearchResult(
            documents = response.documents.map { kakaoPlace ->
                MapContract.LocalSearchResult.Place(
                    id = kakaoPlace.id,
                    placeName = kakaoPlace.placeName,
                    roadAddressName = kakaoPlace.roadAddressName,
                    addressName = kakaoPlace.addressName,
                    longitude = kakaoPlace.longitude,
                    latitude = kakaoPlace.latitude,
                    phone = kakaoPlace.phone,
                    categoryName = kakaoPlace.categoryName,
                )
            },
            searchPaginationMeta = MapContract.LocalSearchResult.SearchPaginationMeta(
                totalCount = response.meta.totalCount,
                pageableCount = response.meta.pageableCount,
                isEnd = response.meta.isEnd,
            ),
        )
    }
}
