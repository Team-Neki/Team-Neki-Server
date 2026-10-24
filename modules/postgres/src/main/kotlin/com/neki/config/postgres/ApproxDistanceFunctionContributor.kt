package com.neki.config.postgres

import org.hibernate.boot.model.FunctionContributions
import org.hibernate.boot.model.FunctionContributor
import org.hibernate.dialect.PostgreSQLDialect
import org.hibernate.type.StandardBasicTypes

/**
 * fileName       : ApproxDistanceFunctionContributor
 * author         : darren
 * date           : 2026. 10. 1.
 * description    : 가까운 순 정렬용 HQL 함수 `approx_distance_order(location, longitude, latitude, longitudeScale)` 를 등록한다.
 *                  경도 차이에 longitudeScale(= cos 위도)을 곱한 평면 거리의 제곱이라 가까운 거리의 순서는 실제 거리와 같다.
 *                  값 자체는 미터가 아니므로 정렬에만 쓴다. ST_DistanceSphere 는 행마다 구면 계산을 해 느리다.
 *                  공간 함수가 없는 H2(테스트)는 항상 0 이 되는 식으로 두어 정렬에 영향을 주지 않는다.
 *                  META-INF/services/org.hibernate.boot.model.FunctionContributor 로 로드된다.
 */
class ApproxDistanceFunctionContributor : FunctionContributor {

    override fun contributeFunctions(functionContributions: FunctionContributions) {
        val pattern: String = if (functionContributions.dialect is PostgreSQLDialect) {
            "(power((ST_X(?1) - ?2) * ?4, 2) + power(ST_Y(?1) - ?3, 2))"
        } else {
            // 숫자 상수 하나만 두면 H2 가 ORDER BY 의 컬럼 번호로 읽는다
            "(?4 * 0)"
        }

        functionContributions.functionRegistry
            .patternDescriptorBuilder(FUNCTION_NAME, pattern)
            .setExactArgumentCount(4)
            .setInvariantType(
                functionContributions.typeConfiguration.basicTypeRegistry.resolve(StandardBasicTypes.DOUBLE),
            )
            .register()
    }

    companion object {
        const val FUNCTION_NAME: String = "approx_distance_order"
    }
}
