package com.yapp2app.map.infra.client.kakao

import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.map.application.contract.LocalSearchResponse
import com.yapp2app.map.application.port.MapApiClientPort
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
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Kakao Local API 키워드 검색
     * @param query 검색 키워드
     * @param page 페이지 번호 (1~45)
     * @param size 한 페이지에 보여질 문서의 개수 (1~15)
     * @param rect 사각형 범위 (x1,y1,x2,y2 - 좌하단 경도,위도,우상단 경도,위도)
     * @return KakaoLocalSearchResponse
     */
    override fun searchByKeyword(query: String, page: Int, size: Int, rect: String?): LocalSearchResponse {
        log.info("Kakao API Request - query: {}, page: {}, size: {}, rect: {}", query, page, size, rect)

        val response = restClient.get()
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
            .body(KakaoLocalSearchResponse::class.java)
            ?: throw BusinessException(ResultCode.ERROR)

        return LocalSearchResponse(
            documents = response.documents.map { kakaoPlace ->
                LocalSearchResponse.Place(
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
            meta = LocalSearchResponse.Meta(
                totalCount = response.meta.totalCount,
                pageableCount = response.meta.pageableCount,
                isEnd = response.meta.isEnd,
            ),
        )
    }
}
