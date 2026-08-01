package com.neki.pose.application.port

/**
 * fileName       : RandomGeneratorPort
 * author         : darren
 * date           : 2026. 1. 29. 11:39
 * description    : bound 내 랜덤 값 추출 Port
 */
interface RandomGeneratorPort {
    fun nextLong(bound: Long): Long
}
