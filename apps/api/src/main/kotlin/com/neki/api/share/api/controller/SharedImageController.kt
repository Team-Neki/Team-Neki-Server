package com.neki.api.share.api.controller

import com.neki.api.share.api.dto.ShareConverter
import com.neki.api.share.application.ViewSharedImageUseCase
import com.neki.api.share.application.dto.ShareResult
import com.neki.domain.share.dto.ShareQuery
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : SharedImageController
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : 공유 링크 이미지 조회 API (공개)
 * - HEAD 는 Spring MVC 가 GET 매핑으로 자동 처리한다 (같은 상태코드·헤더, body 없음).
 * - 추후 OG 랜딩 HTML 을 붙이면 이미지는 /share/{token}/image 로 옮길 수 있다.
 */
@Tag(name = "SharedImageController", description = "공유 링크 이미지 조회 API")
@RestController
@RequestMapping("/share")
class SharedImageController(
    private val viewSharedImageUseCase: ViewSharedImageUseCase,
    private val requestConverter: ShareConverter.RequestConverter,
    private val responseConverter: ShareConverter.ResponseConverter,
) {

    @Operation(
        summary = "공유 이미지 조회",
        description = """공유 토큰으로 사진 이미지를 조회합니다. 인증이 필요 없습니다.
            공유 웹페이지(/s/{token})와 og:image 가 이 주소를 호출합니다.

            - 200: 이미지 바이너리. X-Share-Expires-At 헤더에 링크 만료 일시(ISO-8601, +09:00)를 담습니다.
              Cache-Control 은 max-age=min(남은 시간, 캐시 상한), private 입니다. 캐시 상한은 기본 1시간(설정 app.share.cache-max-age)입니다.
            - 404 (D-19): 토큰 형식이 틀렸거나 없는 토큰입니다.
            - 410 (D-20): 만료된 링크이거나 원본 사진이 삭제됐습니다.""",
    )
    @GetMapping("/{token}")
    fun viewSharedImage(@PathVariable token: String): ResponseEntity<ByteArray> {
        val query: ShareQuery.ViewSharedImage = requestConverter.toViewSharedImageQuery(token)

        val result: ShareResult.ViewSharedImage = viewSharedImageUseCase.execute(query)

        val response: ResponseEntity<ByteArray> = responseConverter.toSharedImageResponse(result)

        return response
    }
}
