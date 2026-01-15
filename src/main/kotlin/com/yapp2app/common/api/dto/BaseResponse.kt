package com.yapp2app.common.api.dto

/**
 * fileName       : BaseResponse
 * author         : darren
 * date           : 2025. 12. 12. 13:24
 * description    :
 */
data class BaseResponse<T>(
    val resultCode: String = ResultCode.SUCCESS.code,
    val message: String = ResultCode.SUCCESS.message,
    val data: T? = null,
)
