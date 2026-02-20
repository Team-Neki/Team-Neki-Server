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

@DisplayName("아키텍처 규칙 검증")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArchitectureRulesTest {

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

        @Test
        fun `Domain 계층은 Application 계층을 의존할 수 없다`() {
            noClasses()
                .that().resideInAnyPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..application..")
                .because("Domain layer must not depend on Application layer (Clean Architecture)")
                .check(importedClasses)
        }

        @Test
        fun `Domain 계층은 도메인별 API 계층을 의존할 수 없다`() {
            noClasses()
                .that().resideInAnyPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                    "com.neki.auth.api..",
                    "com.neki.user.api..",
                    "com.neki.photo.api..",
                    "com.neki.media.api..",
                    "com.neki.pose.api..",
                    "com.neki.map.api..",
                    "com.neki.term.api..",
                    "com.neki.version.api..",
                ).because("Domain layer must not depend on API layer (common.api is allowed)")
                .check(importedClasses)
        }

        @Test
        fun `Domain 계층은 Infra 계층을 의존할 수 없다`() {
            noClasses()
                .that().resideInAnyPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..infra..")
                .because("Domain layer must not depend on Infrastructure layer (Clean Architecture)")
                .check(importedClasses)
        }

        @Test
        fun `Application 계층은 도메인별 API 계층을 의존할 수 없다`() {
            noClasses()
                .that().resideInAnyPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                    "com.neki.auth.api..",
                    "com.neki.user.api..",
                    "com.neki.photo.api..",
                    "com.neki.media.api..",
                    "com.neki.pose.api..",
                    "com.neki.map.api..",
                    "com.neki.term.api..",
                    "com.neki.version.api..",
                ).because("Application layer must not depend on API layer (common.api is allowed)")
                .check(importedClasses)
        }

        // TODO: auth 도메인은 application→infra 의존이 존재 (OauthProperties, UserPrincipal)
        //  장기적으로 리팩토링하여 이 예외를 제거해야 함
        @Test
        fun `Application 계층은 Infra 계층을 의존할 수 없다 (auth 도메인 제외)`() {
            noClasses()
                .that().resideInAnyPackage("..application..")
                .and().resideOutsideOfPackage("com.neki.auth.application..")
                .should().dependOnClassesThat().resideInAnyPackage("..infra..")
                .because("Application layer must not depend on Infrastructure layer (auth domain excluded)")
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
                .should().dependOnClassesThat().resideInAnyPackage(
                    "com.neki.auth.api..",
                    "com.neki.user.api..",
                    "com.neki.photo.api..",
                    "com.neki.media.api..",
                    "com.neki.pose.api..",
                    "com.neki.map.api..",
                    "com.neki.term.api..",
                    "com.neki.version.api..",
                ).because("Infrastructure layer must not depend on domain API packages (common.api is allowed)")
                .check(importedClasses)
        }
    }

    @Nested
    @DisplayName("도메인 격리 규칙")
    inner class DomainIsolation {

        // 위반 없는 도메인에 대해 격리 규칙 적용
        // auth ↔ user: 문서에 명시된 예외 (인증-사용자 결합)
        // map → photo: 기존 위반 (MediaClientPort → MediaStorageInfo), 장기적으로 수정 필요

        private val allDomainPackages = listOf(
            "com.neki.auth",
            "com.neki.user",
            "com.neki.photo",
            "com.neki.media",
            "com.neki.pose",
            "com.neki.map",
            "com.neki.term",
            "com.neki.version",
        )

        private fun otherDomainPackages(domain: String): Array<String> = allDomainPackages
            .filter { !it.endsWith(domain) }
            .map { "$it.." }
            .toTypedArray()

        @Test
        fun `photo 도메인은 다른 도메인에 의존할 수 없다`() {
            noClasses()
                .that().resideInAnyPackage(
                    "com.neki.photo.api..",
                    "com.neki.photo.application..",
                    "com.neki.photo.domain..",
                ).should().dependOnClassesThat().resideInAnyPackage(*otherDomainPackages("photo"))
                .because("photo domain (api/application/domain) must not depend on other domains")
                .check(importedClasses)
        }

        @Test
        fun `media 도메인은 다른 도메인에 의존할 수 없다`() {
            noClasses()
                .that().resideInAnyPackage(
                    "com.neki.media.api..",
                    "com.neki.media.application..",
                    "com.neki.media.domain..",
                ).should().dependOnClassesThat().resideInAnyPackage(*otherDomainPackages("media"))
                .because("media domain (api/application/domain) must not depend on other domains")
                .check(importedClasses)
        }

        @Test
        fun `pose 도메인은 다른 도메인에 의존할 수 없다`() {
            noClasses()
                .that().resideInAnyPackage(
                    "com.neki.pose.api..",
                    "com.neki.pose.application..",
                    "com.neki.pose.domain..",
                ).should().dependOnClassesThat().resideInAnyPackage(*otherDomainPackages("pose"))
                .because("pose domain (api/application/domain) must not depend on other domains")
                .check(importedClasses)
        }

        @Test
        fun `term 도메인은 다른 도메인에 의존할 수 없다`() {
            noClasses()
                .that().resideInAnyPackage(
                    "com.neki.term.api..",
                    "com.neki.term.application..",
                    "com.neki.term.domain..",
                ).should().dependOnClassesThat().resideInAnyPackage(*otherDomainPackages("term"))
                .because("term domain (api/application/domain) must not depend on other domains")
                .check(importedClasses)
        }

        @Test
        fun `version 도메인은 다른 도메인에 의존할 수 없다`() {
            noClasses()
                .that().resideInAnyPackage(
                    "com.neki.version.api..",
                    "com.neki.version.application..",
                    "com.neki.version.domain..",
                ).should().dependOnClassesThat().resideInAnyPackage(*otherDomainPackages("version"))
                .because("version domain (api/application/domain) must not depend on other domains")
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
    @DisplayName("포트/어댑터 패턴 규칙")
    inner class PortAdapterPattern {

        // TODO: auth 도메인은 UseCase→infra 의존이 존재 (OauthProperties, UserPrincipal)
        //  장기적으로 리팩토링하여 이 예외를 제거해야 함
        @Test
        fun `@UseCase 클래스는 infra 계층을 의존할 수 없다 (auth 도메인 제외)`() {
            noClasses()
                .that().areAnnotatedWith(com.neki.common.annotation.UseCase::class.java)
                .and().resideOutsideOfPackage("com.neki.auth..")
                .should().dependOnClassesThat().resideInAnyPackage("..infra..")
                .because("@UseCase classes must depend on ports, not infrastructure (auth domain excluded)")
                .check(importedClasses)
        }
    }
}
