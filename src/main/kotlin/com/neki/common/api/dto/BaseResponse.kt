package com.neki.common.api.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * fileName       : BaseResponse
 * author         : darren
 * date           : 2025. 12. 12. 13:24
 * description    :
 */
data class BaseResponse<T>(
    @field:Schema(description = "응답 코드", example = "D-0")
    val resultCode: String = ResultCode.SUCCESS.code,
    @field:Schema(description = "응답 메시지", example = "OK")
    val message: String = ResultCode.SUCCESS.message,
    val data: T? = null,
)
