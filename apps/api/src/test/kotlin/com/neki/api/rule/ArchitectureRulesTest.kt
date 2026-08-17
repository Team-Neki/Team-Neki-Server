package com.neki.api.rule

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("아키텍처 규칙 검증")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArchitectureRulesTest {

    companion object {
        // 한 도메인은 두 루트에 걸쳐 있다. :domain 은 com.neki.domain.<name>, :apps:api 는 com.neki.api.<name>.
        private val ALL_DOMAIN_NAMES = listOf(
            "user",
            "photo",
            "media",
            "pose",
            "map",
            "support",
            "notification",
        )

        /** 격리 검증 대상 도메인 (user는 알려진 예외로 제외) */
        private val ISOLATED_DOMAINS = listOf("photo", "media", "pose", "map", "support", "notification")

        // 앱 루트가 com.neki.api 라서 "..api.." 는 :apps:api 전체와 매칭된다.
        // 계층으로서의 api 를 가리킬 때는 반드시 아래 상수를 쓴다.
        private const val API_LAYER = "com.neki.api.*.api.."
        private const val APPLICATION_LAYER = "com.neki.api.*.application.."

        /** :core 가 제공하는 공용 API 계약 (BaseResponse 등) */
        private const val CORE_API_LAYER = "com.neki.core.api.."

        private fun domainApiPackages(): Array<String> = ALL_DOMAIN_NAMES
            .map { "com.neki.api.$it.api.." }
            .toTypedArray()

        private fun otherDomainPackages(domain: String): Array<String> = ALL_DOMAIN_NAMES
            .filter { it != domain }
            .flatMap { listOf("com.neki.api.$it..", "com.neki.domain.$it..") }
            .toTypedArray()

        private fun allDomainPackages(): Array<String> = ALL_DOMAIN_NAMES
            .flatMap { listOf("com.neki.api.$it..", "com.neki.domain.$it..") }
            .toTypedArray()

        @JvmStatic
        fun isolatedDomains(): List<String> = ISOLATED_DOMAINS

        @JvmStatic
        fun allDomains(): List<String> = ALL_DOMAIN_NAMES
    }

    private lateinit var importedClasses: JavaClasses

    @BeforeAll
    fun setup() {
        importedClasses = ClassFileImporter()
            .withImportOption(DoNotIncludeTests())
            .importPackages("com.neki")
    }

    @Nested
    @DisplayName("레이어 의존성 규칙")
    inner class LayerDependencies {

        // Domain 계층(:domain 의 entity/enums/vo)의 application/api 의존 금지는
        // Gradle 모듈 그래프가 보장한다 (:domain 은 :apps:api 를 의존하지 않으므로 컴파일 자체가 불가능).
        // :domain 은 자신의 infra(기술 구현 어댑터)를 포함하므로, domain 서비스가 자기 도메인 infra를
        // 의존하지 않는지는 "도메인 격리 규칙"의 별도 테스트로 검증한다.
        // 같은 모듈 안에서 발생할 수 있는 도메인 간 엔티티 의존은 "도메인 격리 규칙"에서 검증한다.

        @Test
        fun `Application 계층은 도메인별 API 계층을 의존할 수 없다`() {
            noClasses()
                .that().resideInAnyPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(*domainApiPackages())
                .because("Application layer must not depend on API layer (common.api is allowed)")
                .check(importedClasses)
        }

        // TODO: user 도메인은 application→infra 의존이 존재 (OauthProperties, UserPrincipal)
        //  장기적으로 리팩토링하여 이 예외를 제거해야 함
        @Test
        fun `Application 계층은 Infra 계층을 의존할 수 없다 (user 도메인 제외)`() {
            noClasses()
                .that().resideInAnyPackage("..application..")
                .and().resideOutsideOfPackage("com.neki.api.user.application..")
                .should().dependOnClassesThat().resideInAnyPackage("..infra..")
                .because("Application layer must not depend on Infrastructure layer (user domain excluded)")
                .check(importedClasses)
        }

        // 앱 루트가 com.neki.api 이므로 ..api.. 는 :apps:api 전체와 매칭된다.
        // 계층으로서의 api 만 가리키도록 com.neki.api.<도메인>.api.. 로 고정한다.
        @Test
        fun `API 계층은 Infra 계층을 의존할 수 없다`() {
            noClasses()
                .that().resideInAnyPackage(API_LAYER)
                .and().resideOutsideOfPackage("com.neki.api.common.api..")
                .should().dependOnClassesThat().resideInAnyPackage("..infra..")
                .because("API layer must not depend on Infrastructure layer (Clean Architecture)")
                .check(importedClasses)
        }
    }

    @Nested
    @DisplayName("Infra 레이어 규칙")
    inner class InfraLayerRules {

        @Test
        fun `Infra 계층은 도메인별 API 계층을 의존할 수 없다`() {
            noClasses()
                .that().resideInAnyPackage("..infra..")
                .should().dependOnClassesThat().resideInAnyPackage(*domainApiPackages())
                .because("Infrastructure layer must not depend on domain API packages (common.api is allowed)")
                .check(importedClasses)
        }
    }

    @Nested
    @DisplayName("도메인 격리 규칙")
    inner class DomainIsolation {

        // 위반 없는 도메인에 대해 격리 규칙 적용
        // user: auth 통합으로 인한 예외

        @ParameterizedTest(name = "{0} 도메인은 다른 도메인에 의존할 수 없다")
        @MethodSource("com.neki.api.rule.ArchitectureRulesTest#isolatedDomains")
        fun `격리된 도메인은 다른 도메인에 의존할 수 없다`(domain: String) {
            noClasses()
                .that().resideInAnyPackage(
                    "com.neki.api.$domain.api..",
                    "com.neki.api.$domain.application..",
                ).should().dependOnClassesThat().resideInAnyPackage(*otherDomainPackages(domain))
                .because("$domain domain (api/application) must not depend on other domains")
                .check(importedClasses)
        }

        // :domain 은 모든 도메인이 한 모듈에 모여 있어 Gradle이 도메인 간 의존을 막지 못한다.
        // user 의 알려진 예외는 application 계층 문제이므로 :domain 계층은 전 도메인을 검증한다.
        // 루트 패키지(com.neki.domain.<domain>)는 BrandOrderPolicy 같은 도메인 정책 object 를 포함한다.
        // 인터페이스는 repository/client/external 로 나뉘며, 셋 다 자기 도메인 models 만 주고받아야 한다.
        @ParameterizedTest(name = "{0} 도메인의 domain 모듈 계층은 다른 도메인에 의존할 수 없다")
        @MethodSource("com.neki.api.rule.ArchitectureRulesTest#allDomains")
        fun `domain 모듈 계층은 다른 도메인에 의존할 수 없다`(domain: String) {
            noClasses()
                .that().resideInAnyPackage(
                    "com.neki.domain.$domain",
                    "com.neki.domain.$domain.models..",
                    "com.neki.domain.$domain.dto..",
                    "com.neki.domain.$domain.service..",
                    "com.neki.domain.$domain.repository..",
                    "com.neki.domain.$domain.client..",
                    "com.neki.domain.$domain.external..",
                    "com.neki.domain.$domain.infra..",
                ).should().dependOnClassesThat().resideInAnyPackage(*otherDomainPackages(domain))
                .because("$domain domain module must not depend on other domains")
                .check(importedClasses)
        }

        // :domain 은 자신의 infra(기술 구현 어댑터)를 포함한다. 도메인 서비스는 자기 도메인의
        // repository/client/external 인터페이스만 알아야 하고, 그 인터페이스를 구현하는 구체 어댑터를
        // 직접 의존해서는 안 된다 (docs/layering-policy.md 의 "domain에서 infra 타입을 import하는 것" 금지).
        @ParameterizedTest(name = "{0} 도메인의 domain 계층은 자기 도메인 infra를 의존할 수 없다")
        @MethodSource("com.neki.api.rule.ArchitectureRulesTest#allDomains")
        fun `domain 계층은 자기 도메인의 infra를 의존할 수 없다`(domain: String) {
            noClasses()
                .that().resideInAnyPackage(
                    "com.neki.domain.$domain",
                    "com.neki.domain.$domain.models..",
                    "com.neki.domain.$domain.dto..",
                    "com.neki.domain.$domain.service..",
                    "com.neki.domain.$domain.repository..",
                    "com.neki.domain.$domain.client..",
                    "com.neki.domain.$domain.external..",
                ).should().dependOnClassesThat().resideInAnyPackage("com.neki.domain.$domain.infra..")
                .because("domain layer must depend on its own interfaces, not concrete infra adapters")
                .check(importedClasses)
        }
    }

    @Nested
    @DisplayName("어노테이션 배치 규칙")
    inner class AnnotationPlacement {

        @Test
        fun `@UseCase는 application 패키지에만 위치해야 한다`() {
            classes()
                .that().areAnnotatedWith(com.neki.core.annotation.UseCase::class.java)
                .should().resideInAnyPackage("..application..")
                .because("@UseCase annotation should only be used in application packages")
                .check(importedClasses)
        }

        @Test
        fun `@RestController는 api controller 패키지에만 위치해야 한다`() {
            classes()
                .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController::class.java)
                .should().resideInAnyPackage("..api.controller..")
                .because("@RestController should only be used in api.controller packages")
                .check(importedClasses)
        }

        @Test
        fun `@Repository는 infra 패키지에만 위치해야 한다`() {
            classes()
                .that().areAnnotatedWith(org.springframework.stereotype.Repository::class.java)
                .should().resideInAnyPackage("..infra..")
                .because("@Repository should only be used in infra packages")
                .check(importedClasses)
        }
    }

    @Nested
    @DisplayName("DTO 배치 규칙")
    inner class DtoPlacement {

        // 이 규칙들은 이름 접미사만 보고 판단하므로, 접미사가 우연히 겹치는 공유 커널 값 객체까지 걸린다.
        // (예: com.neki.core.domain.vo.Pagination 이전의 PageRequest)
        // 공유 커널은 특정 계층의 DTO가 아니므로 검사 대상에서 제외한다.
        private val sharedKernel = "com.neki.core.domain.."

        @Test
        fun `Request 클래스는 api 계층에만 위치해야 한다`() {
            classes()
                .that().haveSimpleNameEndingWith("Request")
                .and().resideOutsideOfPackage(sharedKernel)
                .should().resideInAnyPackage(API_LAYER, CORE_API_LAYER)
                .because("Request DTOs should only be in the API layer")
                .check(importedClasses)
        }

        @Test
        fun `Response 클래스는 api 계층에만 위치해야 한다`() {
            classes()
                .that().haveSimpleNameEndingWith("Response")
                .and().resideOutsideOfPackage(sharedKernel)
                .should().resideInAnyPackage(API_LAYER, CORE_API_LAYER)
                .because("Response DTOs should only be in the API layer")
                .check(importedClasses)
        }

        // 유스케이스 입력(Command/Query)은 :domain 의 com.neki.api.<domain>.dto 로 이관이 끝났다.
        @Test
        fun `Command 클래스는 도메인 dto 패키지에만 위치해야 한다`() {
            classes()
                .that().haveSimpleNameEndingWith("Command")
                .and().resideOutsideOfPackage(sharedKernel)
                .should().resideInAnyPackage("com.neki.domain.*.dto..")
                .because("Command DTOs should be in the domain dto package")
                .check(importedClasses)
        }

        @Test
        fun `Query 클래스는 도메인 dto 패키지에만 위치해야 한다`() {
            classes()
                .that().haveSimpleNameEndingWith("Query")
                .and().areTopLevelClasses()
                .and().resideOutsideOfPackage(sharedKernel)
                .should().resideInAnyPackage("com.neki.domain.*.dto..")
                .because("Query DTOs should be in the domain dto package")
                .check(importedClasses)
        }

        // 유스케이스 출력(Result)은 application 계층에 둔다.
        // 중첩 타입은 제외한다 (port 계약의 중첩 클래스는 Result DTO가 아님)
        @Test
        fun `Result 클래스는 application 계층에만 위치해야 한다`() {
            classes()
                .that().haveSimpleNameEndingWith("Result")
                .and().areTopLevelClasses()
                .and().resideOutsideOfPackage(sharedKernel)
                .should().resideInAnyPackage("..application..")
                .because("Result DTOs should be in the Application layer")
                .check(importedClasses)
        }
    }

    @Nested
    @DisplayName("모듈 의존 방향 규칙")
    inner class ModuleDependencyRules {

        /**
         * :core 공유 커널(:core 전용 패키지)은 바깥 레이어(api/infra/application)를 의존할 수 없다.
         * - annotation: UseCase (com.neki.core.annotation)
         * - domain: BaseTimeEntity, vo.SortOrder (com.neki.core.domain..)
         * - transaction: TransactionRunner (com.neki.core.transaction)
         * - api.dto: BaseResponse (com.neki.core.api.dto, :core 전용)
         * - code: ResultCode (com.neki.core.code)
         * - exception: BusinessException (com.neki.core.exception, exact — exception.handler는 :application 소속)
         *
         * 주의: com.neki.api.common.api.config/document, com.neki.api.common.exception.handler,
         *       com.neki.api.common.filter/properties 는 :application 소속이므로 커널에 포함하지 않는다.
         *
         * 바깥 레이어는 :core 기준 ..infra.. (modules:*) 와 ..application.. 이다.
         * (..api.. 는 커널 자신의 api.dto 를 포함하므로 금지 대상에서 제외)
         */
        @Test
        fun `core 공유 커널은 바깥 레이어를 의존할 수 없다`() {
            noClasses()
                .that().resideInAnyPackage(
                    "com.neki.core.annotation..",
                    "com.neki.core.domain..",
                    "com.neki.core.transaction..",
                    "com.neki.core.api.dto..",
                    "com.neki.core.code..",
                    "com.neki.core.exception",
                ).should().dependOnClassesThat().resideInAnyPackage(
                    "..infra..",
                    "..application..",
                    "com.neki.config..",
                ).because("core shared kernel must not depend on outer layers (infra/application/module config)")
                .check(importedClasses)
        }

        /**
         * :modules:* 의 연결 설정(com.neki.config..)은 infra 어댑터에서만 참조할 수 있다.
         * api/application 이 모듈 설정을 직접 알면 인프라 교체 시 영향이 계층을 넘어 번진다.
         * (S3Properties, KakaoApiRateLimitProperties 등은 어댑터 와이어링 용도로만 쓰인다)
         */
        @Test
        fun `api·application 레이어는 모듈 연결설정을 의존할 수 없다`() {
            noClasses()
                .that().resideInAnyPackage(API_LAYER, APPLICATION_LAYER)
                .should().dependOnClassesThat().resideInAnyPackage("com.neki.config..")
                .because("module connection settings must be referenced only by infra adapters")
                .check(importedClasses)
        }

        /**
         * :modules:* 의 연결 설정은 도메인 코드를 몰라야 한다.
         * 모듈은 의존성과 설정만 관리하므로 도메인 방향 의존이 생기면 안 된다.
         */
        @Test
        fun `모듈 연결설정은 도메인을 의존할 수 없다`() {
            noClasses()
                .that().resideInAnyPackage("com.neki.config..")
                .should().dependOnClassesThat().resideInAnyPackage(*allDomainPackages())
                .because("module config must not depend on domain code")
                .check(importedClasses)
        }

        /**
         * :application 레이어는 :modules:* 의 infra 어댑터(..infra..)를 의존할 수 없다.
         * application은 port 만 알아야 한다. (user 도메인은 알려진 예외)
         * 기존 LayerDependencies 규칙을 모듈 관점에서 재확인한다.
         */
        @Test
        fun `application 레이어는 infra 어댑터를 의존할 수 없다 (user 도메인 제외)`() {
            noClasses()
                .that().resideInAnyPackage("..application..")
                .and().resideOutsideOfPackage("com.neki.api.user.application..")
                .should().dependOnClassesThat().resideInAnyPackage("..infra..")
                .because("application layer must know ports only, not module infra adapters (user domain excluded)")
                .check(importedClasses)
        }
    }

    @Nested
    @DisplayName("포트/어댑터 패턴 규칙")
    inner class PortAdapterPattern {

        // TODO: user 도메인은 UseCase→infra 의존이 존재 (OauthProperties, UserPrincipal)
        //  장기적으로 리팩토링하여 이 예외를 제거해야 함
        @Test
        fun `@UseCase 클래스는 infra 계층을 의존할 수 없다 (user 도메인 제외)`() {
            noClasses()
                .that().areAnnotatedWith(com.neki.core.annotation.UseCase::class.java)
                .and().resideOutsideOfPackage("com.neki.api.user..")
                .should().dependOnClassesThat().resideInAnyPackage("..infra..")
                .because("@UseCase classes must depend on ports, not infrastructure (user domain excluded)")
                .check(importedClasses)
        }
    }
}
