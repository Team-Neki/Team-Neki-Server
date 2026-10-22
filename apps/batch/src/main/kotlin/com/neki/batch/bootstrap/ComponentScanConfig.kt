package com.neki.batch.bootstrap

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * fileName       : ComponentScanConfig
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : batch 가 의존하는 패키지 목록. 스캔 범위가 곧 batch 가 쓰는 도메인·모듈 목록이다.
 *
 * 스캔 범위는 필요한 것만으로 좁힌다. com.neki 전체를 스캔하면 :domain 의 user/infra/security 가
 * apps/api 에만 있는 CorsConfigurationSource 를 요구하고, modules 의 aws/redis 연결 설정이
 * 각자의 yaml 과 외부 시스템을 요구해 기동이 깨진다.
 * - searchIndexJob : search 도메인과 map 의 persist 어댑터(BrandRepository). map 의 kakao/infra 는 외부 API 설정을 요구하므로 넣지 않는다
 * - 알림 발송 잡 3종 : notification 도메인 전부(모델, 포트, 푸시·공휴일 어댑터), photo 의 persist 어댑터(업로드 집계),
 *   firebase 연결 설정(FirebaseMessaging 빈. 키 파일이 있을 때만 뜬다)
 * 새 잡이 다른 도메인을 쓰면 여기에 그 패키지를 더한다. NekiBatchApplication 은 자기 패키지(com.neki.batch)만 스캔한다
 */
@Configuration
@ComponentScan(
    basePackages = [
        "com.neki.core",
        "com.neki.config.postgres",
        "com.neki.config.jasypt",
        "com.neki.config.firebase",
        "com.neki.domain.search",
        "com.neki.domain.map.infra.persist",
        "com.neki.domain.notification",
        "com.neki.domain.photo.infra.persist",
    ],
)
@EntityScan("com.neki.domain", "com.neki.core")
@EnableJpaRepositories("com.neki.domain")
class ComponentScanConfig
