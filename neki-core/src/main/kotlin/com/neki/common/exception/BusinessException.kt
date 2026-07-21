package com.neki.common.exception

import com.neki.common.code.ResultCode

/**
 * fileName       : BusinessException
 * author         : darren
 * date           : 2025. 12. 12. 13:30
 * description    : Service Layer에서 발생하는 예외
 */
class BusinessException(val resultCode: ResultCode) : RuntimeException(resultCode.message)
