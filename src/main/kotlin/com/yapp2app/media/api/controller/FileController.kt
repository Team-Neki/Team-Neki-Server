package com.yapp2app.media.api.controller

import com.yapp2app.media.api.converter.FileCommandConverter
import com.yapp2app.media.api.converter.FileResultConverter
import com.yapp2app.media.application.usecase.GetImageByKeyUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : FileController
 * author         : koo
 * date           : 2026. 1. 21.
 * description    : 정적 파일(이미지) 조회 API - 프론트엔드 img 태그의 src로 사용
 */
@Tag(name = "FileController", description = "정적 파일 조회 API")
@RestController
@RequestMapping("/file")
class FileController(
    private val getImageByKeyUseCase: GetImageByKeyUseCase,
    private val commandConverter: FileCommandConverter,
    private val resultConverter: FileResultConverter,
) {

    @Operation(
        summary = "이미지 파일 조회",
        description = """
            object-key를 이용하여 이미지 파일을 조회합니다.
            프론트엔드에서 <img src="/file/image/{object-key}"> 형태로 사용합니다.

            캐싱 전략:
            - HTTP 캐시 헤더 설정 (Cache-Control: max-age=86400)
        """,
    )
    @GetMapping("/image/**")
    fun getImage(request: HttpServletRequest): ResponseEntity<ByteArray> {
        val objectKey = extractObjectKey(request)

        val command = commandConverter.toGetImageByKeyCommand(objectKey)

        val result = getImageByKeyUseCase.execute(command)

        val response = resultConverter.toImageResponse(result)

        return response
    }

    private fun extractObjectKey(request: HttpServletRequest): String {
        val fullPath = request.requestURI
        return fullPath.removePrefix("/file/image/")
    }
}
