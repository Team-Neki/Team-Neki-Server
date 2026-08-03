package com.neki.photo.dto

/**
 * fileName       : UserScoped
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 소유자 기준으로 처리되는 command/query.
 * 도메인 서비스가 command/query를 그대로 받되, 소유자만 필요한 경우 이 계약으로 받는다.
 */
interface UserScoped {
    val userId: Long
}
