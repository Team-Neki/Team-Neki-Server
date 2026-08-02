package com.neki.rule

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
        private val ALL_DOMAIN_PACKAGES = listOf(
            "com.neki.user",
            "com.neki.photo",
            "com.neki.media",
            "com.neki.pose",
            "com.neki.map",
            "com.neki.support",
            "com.neki.notification",
        )

        /** 격리 검증 대상 도메인 (user, map은 알려진 예외로 제외) */
        private val ISOLATED_DOMAINS = listOf("photo", "media", "pose", "support", "notification")

        private fun domainApiPackages(): Array<String> = ALL_DOMAIN_PACKAGES.map { "$it.api.." }.toTypedArray()

        private fun otherDomainPackages(domain: String): Array<String> = ALL_DOMAIN_PACKAGES
            .filter { !it.endsWith(domain) }
            .map { "$it.." }
            .toTypedArray()

        @JvmStatic
        fun isolatedDomains(): List<String> = ISOLATED_DOMAINS

        @JvmStatic
        fun allDomains(): List<String> = ALL_DOMAIN_PACKAGES.map { it.substringAfterLast('.') }
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

        // Domain 계층(:domain 의 entity/enums/vo)의 바깥 레이어(application/api/infra) 의존 금지는
        // Gradle 모듈 그래프가 보장한다 (:domain 은 :core 만 의존하므로 컴파일 자체가 불가능).
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
                .and().resideOutsideOfPackage("com.neki.user.application..")
                .should().dependOnClassesThat().resideInAnyPackage("..infra..")
                .because("Application layer must not depend on Infrastructure layer (user domain excluded)")
                .check(importedClasses)
        }

        @Test
        fun `API 계층은 Infra 계층을 의존할 수 없다`() {
            noClasses()
                .that().resideInAnyPackage("..api..")
                .and().resideOutsideOfPackage("com.neki.common.api..")
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
        // map → photo: 기존 위반 (MediaClientPort → MediaStorageInfo), 장기적으로 수정 필요

        @ParameterizedTest(name = "{0} 도메인은 다른 도메인에 의존할 수 없다")
        @MethodSource("com.neki.rule.ArchitectureRulesTest#isolatedDomains")
        fun `격리된 도메인은 다른 도메인에 의존할 수 없다`(domain: String) {
            noClasses()
                .that().resideInAnyPackage(
                    "com.neki.$domain.api..",
                    "com.neki.$domain.application..",
                ).should().dependOnClassesThat().resideInAnyPackage(*otherDomainPackages(domain))
                .because("$domain domain (api/application) must not depend on other domains")
                .check(importedClasses)
        }

        // 엔티티 계층은 :domain 한 모듈에 모여 있어 Gradle이 도메인 간 의존을 막지 못한다.
        // user, map 의 알려진 예외는 application 계층 문제이므로 엔티티 계층은 전 도메인을 검증한다.
        // 루트 패키지(com.neki.<domain>)는 MediaType, HeadCount 등 엔티티 부속 클래스를 포함한다.
        @ParameterizedTest(name = "{0} 도메인 엔티티 계층은 다른 도메인에 의존할 수 없다")
        @MethodSource("com.neki.rule.ArchitectureRulesTest#allDomains")
        fun `엔티티 계층은 다른 도메인에 의존할 수 없다`(domain: String) {
            noClasses()
                .that().resideInAnyPackage(
                    "com.neki.$domain",
                    "com.neki.$domain.entity..",
                    "com.neki.$domain.enums..",
                    "com.neki.$domain.vo..",
                ).should().dependOnClassesThat().resideInAnyPackage(*otherDomainPackages(domain))
                .because("$domain entity layer must not depend on other domains")
                .check(importedClasses)
        }
    }

    @Nested
    @DisplayName("어노테이션 배치 규칙")
    inner class AnnotationPlacement {

        @Test
        fun `@UseCase는 application usecase 패키지에만 위치해야 한다`() {
            classes()
                .that().areAnnotatedWith(com.neki.common.annotation.UseCase::class.java)
                .should().resideInAnyPackage("..application.usecase..")
                .because("@UseCase annotation should only be used in application.usecase packages")
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

        @Test
        fun `Request 클래스는 api 계층에만 위치해야 한다`() {
            classes()
                .that().haveSimpleNameEndingWith("Request")
                .should().resideInAnyPackage("..api..")
                .because("Request DTOs should only be in the API layer")
                .check(importedClasses)
        }

        @Test
        fun `Response 클래스는 api 계층에만 위치해야 한다`() {
            classes()
                .that().haveSimpleNameEndingWith("Response")
                .should().resideInAnyPackage("..api..")
                .because("Response DTOs should only be in the API layer")
                .check(importedClasses)
        }

        @Test
        fun `Command 클래스는 application 계층에만 위치해야 한다`() {
            classes()
                .that().haveSimpleNameEndingWith("Command")
                .should().resideInAnyPackage("..application..")
                .because("Command DTOs should only be in the Application layer")
                .check(importedClasses)
        }

        @Test
        fun `Result 클래스는 application 계층에만 위치해야 한다`() {
            classes()
                .that().haveSimpleNameEndingWith("Result")
                .should().resideInAnyPackage("..application..")
                .because("Result DTOs should only be in the Application layer")
                .check(importedClasses)
        }
    }

    @Nested
    @DisplayName("모듈 의존 방향 규칙")
    inner class ModuleDependencyRules {

        /**
         * :core 공유 커널(:core 전용 패키지)은 바깥 레이어(api/infra/application)를 의존할 수 없다.
         * - annotation: UseCase (com.neki.common.annotation)
         * - domain: BaseTimeEntity, vo.SortOrder (com.neki.common.domain..)
         * - transaction: TransactionRunner (com.neki.common.transaction)
         * - api.dto: BaseResponse (com.neki.common.api.dto, :core 전용)
         * - code: ResultCode (com.neki.common.code)
         * - exception: BusinessException (com.neki.common.exception, exact — exception.handler는 :application 소속)
         *
         * 주의: com.neki.common.api.config/document, com.neki.common.exception.handler,
         *       com.neki.common.filter/properties 는 :application 소속이므로 커널에 포함하지 않는다.
         *
         * 바깥 레이어는 :core 기준 ..infra.. (modules:*) 와 ..application.. 이다.
         * (..api.. 는 커널 자신의 api.dto 를 포함하므로 금지 대상에서 제외)
         */
        @Test
        fun `core 공유 커널은 바깥 레이어를 의존할 수 없다`() {
            noClasses()
                .that().resideInAnyPackage(
                    "com.neki.common.annotation..",
                    "com.neki.common.domain..",
                    "com.neki.common.transaction..",
                    "com.neki.common.api.dto..",
                    "com.neki.common.code..",
                    "com.neki.common.exception",
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
                .that().resideInAnyPackage("..api..", "..application..")
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
                .should().dependOnClassesThat().resideInAnyPackage(*ALL_DOMAIN_PACKAGES.map { "$it.." }.toTypedArray())
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
                .and().resideOutsideOfPackage("com.neki.user.application..")
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
                .that().areAnnotatedWith(com.neki.common.annotation.UseCase::class.java)
                .and().resideOutsideOfPackage("com.neki.user..")
                .should().dependOnClassesThat().resideInAnyPackage("..infra..")
                .because("@UseCase classes must depend on ports, not infrastructure (user domain excluded)")
                .check(importedClasses)
        }
    }
}
