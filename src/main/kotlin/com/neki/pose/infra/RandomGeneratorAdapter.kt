package com.neki.pose.infra

import com.neki.pose.application.port.RandomGeneratorPort
import org.springframework.stereotype.Component
import kotlin.random.Random

/**
 * fileName       : RandomGeneratorAdapter
 * author         : darren
 * date           : 2026. 1. 29. 11:41
 * description    : bound 내 랜덤 값 추출
 */
@Component
class RandomGeneratorAdapter : RandomGeneratorPort {
    override fun nextLong(bound: Long): Long = Random.nextLong(bound)
}
