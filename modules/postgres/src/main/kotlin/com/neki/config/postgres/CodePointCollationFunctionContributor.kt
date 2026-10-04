package com.neki.config.postgres

import org.hibernate.boot.model.FunctionContributions
import org.hibernate.boot.model.FunctionContributor
import org.hibernate.dialect.PostgreSQLDialect
import org.hibernate.type.StandardBasicTypes

/**
 * fileName       : CodePointCollationFunctionContributor
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : 문자열을 코드포인트 순으로 비교하게 하는 HQL 함수 `code_point_collate(x)` 를 등록한다.
 *                  DB 기본 collation(en_US.utf8)은 한글 비교에서 `강남대` 를 `강남구청` 앞에 둔다.
 *                  PostgreSQL 은 `x collate ucs_basic` 으로 렌더링하고, COLLATE 를 지원하지 않는 H2(테스트)는
 *                  원래 코드포인트 순으로 비교하므로 x 를 그대로 둔다.
 *                  META-INF/services/org.hibernate.boot.model.FunctionContributor 로 로드된다.
 */
class CodePointCollationFunctionContributor : FunctionContributor {

    override fun contributeFunctions(functionContributions: FunctionContributions) {
        val pattern: String = if (functionContributions.dialect is PostgreSQLDialect) {
            "(?1 collate ucs_basic)"
        } else {
            "(?1)"
        }

        functionContributions.functionRegistry
            .patternDescriptorBuilder(FUNCTION_NAME, pattern)
            .setExactArgumentCount(1)
            .setInvariantType(
                functionContributions.typeConfiguration.basicTypeRegistry.resolve(StandardBasicTypes.STRING),
            )
            .register()
    }

    companion object {
        const val FUNCTION_NAME: String = "code_point_collate"
    }
}
