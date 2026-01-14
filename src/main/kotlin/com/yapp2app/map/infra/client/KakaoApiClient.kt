package com.yapp2app.map.infra.client

import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.map.application.contract.KakaoLocalSearchResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.concurrent.TimeUnit

/**
 * fileName       : KakaoApiClient
 * author         : darren
 * date           : 2026. 01. 13.
 * description    : Kakao Local API 클라이언트
 */
@Component
class KakaoApiClient(
    @Value("\${kakao.api.key}")
    private val apiKey: String,

    private val restClient: RestClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Kakao Local API 키워드 검색
     * @param query 검색 키워드
     * @param page 페이지 번호 (1~45)
     * @param size 한 페이지에 보여질 문서의 개수 (1~15)
     * @param rect 사각형 범위 (x1,y1,x2,y2 - 좌하단 경도,위도,우상단 경도,위도)
     * @return KakaoLocalSearchResponse
     */
    fun searchByKeyword(query: String, page: Int = 1, size: Int = 15, rect: String? = null): KakaoLocalSearchResponse {
        log.info("Kakao API Request - query: {}, page: {}, size: {}, rect: {}", query, page, size, rect)

        // Rate limiting: 요청 사이에 랜덤 지연 (300ms ~ 700ms)
        val delayMillis = (300L..700L).random()
        TimeUnit.MILLISECONDS.sleep(delayMillis)

        return restClient.get()
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
    }
}
