package com.yapp2app.common.exception.dto

import java.io.Serializable

/**
 * fileName       : ExceptionDto
 * author         : darren
 * date           : 2025. 12. 12. 13:27
 * description    :
 */
data class FieldErrorDetail(var field: String, var message: String)

data class ExceptionMsg(
    val message: String,
    val code: String,
    val success: Boolean,
    val errors: List<FieldErrorDetail>? = null,
) : Serializable
