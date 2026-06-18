package com.neki.media.api.controller

import com.neki.common.api.document.RequiresSecurity
import com.neki.common.api.dto.BaseResponse
import com.neki.media.application.result.BackfillMediaDimensionsResult
import com.neki.media.application.usecase.BackfillMediaDimensionsUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : MediaAdminController
 * author         : darren
 * date           : 2026. 6. 18.
 * description    : 미디어 운영/백필용 admin API
 */
@Tag(name = "MediaAdminController", description = "미디어 운영/백필 API")
@RequiresSecurity
@RestController
@RequestMapping("/api/media/admin")
class MediaAdminController(private val backfillMediaDimensionsUseCase: BackfillMediaDimensionsUseCase) {

    @Operation(
        summary = "미디어 width/height/size 백필",
        description = """
            width/height/size 컬럼이 추가되기 전 업로드된(기존) 미디어의 누락된 값을 S3 원본에서 계산해 채웁니다.

            * 대상: status = UPLOADED 이면서 width/height/size 중 하나라도 null 인 미디어
            * size: S3 객체 바이트 길이
            * width/height: 이미지 헤더에서 추출 (디코딩 불가 시 해당 값만 null 유지)
            * 이미 채워진 값은 덮어쓰지 않으며, 반복 호출해도 안전(idempotent)합니다.
        """,
    )
    @PostMapping("/dimensions/backfill")
    fun backfillDimensions(
        @RequestParam(defaultValue = "100") batchSize: Int,
    ): BaseResponse<BackfillMediaDimensionsResult> {
        val result: BackfillMediaDimensionsResult = backfillMediaDimensionsUseCase.execute(batchSize)
        return BaseResponse(data = result)
    }
}
