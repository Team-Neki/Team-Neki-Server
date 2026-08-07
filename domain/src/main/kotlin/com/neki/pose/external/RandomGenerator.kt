package com.neki.pose.external

/**
 * fileName       : RandomGenerator
 * author         : darren
 * date           : 2026. 1. 29. 11:39
 * description    : bound 내 랜덤 값 추출 Port
 */
interface RandomGenerator {
    fun nextLong(bound: Long): Long
}
