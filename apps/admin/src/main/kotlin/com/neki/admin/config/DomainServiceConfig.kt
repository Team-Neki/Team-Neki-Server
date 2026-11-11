package com.neki.admin.config

import com.neki.domain.map.repository.BrandRepository
import com.neki.domain.map.service.BrandService
import com.neki.domain.pose.repository.PoseRepository
import com.neki.domain.pose.service.PoseService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * fileName       : DomainServiceConfig
 * author         : koo
 * date           : 2026. 8. 10.
 * description    : admin 이 쓰는 도메인 서비스만 명시적으로 등록한다
 *
 * :domain 을 패키지 단위로 스캔하면 admin 이 쓰지 않는 도메인 서비스까지 빈으로 올라오고,
 * 그 서비스들의 포트 어댑터를 전부 요구해 기동이 실패한다.
 */
@Configuration
class DomainServiceConfig {

    @Bean
    fun brandService(brandRepository: BrandRepository): BrandService = BrandService(brandRepository)

    @Bean
    fun poseService(poseRepository: PoseRepository): PoseService = PoseService(poseRepository)
}
