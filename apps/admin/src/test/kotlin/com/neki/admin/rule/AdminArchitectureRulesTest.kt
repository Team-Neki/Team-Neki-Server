package com.neki.admin.rule

import com.neki.core.annotation.UseCase
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.repository.Repository
import org.springframework.stereotype.Service

// api 앱은 application 계층에 @UseCase 를 쓰지만 admin 은 @Service 를 쓴다.
@DisplayName("admin 아키텍처 규칙 검증")
class AdminArchitectureRulesTest {

    private val importedClasses: JavaClasses = ClassFileImporter()
        .withImportOption(DoNotIncludeTests())
        .importPackages("com.neki.admin")

    @Test
    @DisplayName("application 계층의 클래스는 @Service 를 사용한다")
    fun applicationClassesShouldBeAnnotatedWithService() {
        classes().that().resideInAPackage("com.neki.admin.*.application")
            .and().areTopLevelClasses()
            .should().beAnnotatedWith(Service::class.java)
            .check(importedClasses)
    }

    @Test
    @DisplayName("admin 에서는 @UseCase 를 사용하지 않는다")
    fun adminShouldNotUseUseCaseAnnotation() {
        noClasses().should().beAnnotatedWith(UseCase::class.java)
            .check(importedClasses)
    }

    @Test
    @DisplayName("application 계층은 infra 와 Spring Data 리포지터리를 직접 쓰지 않고 도메인 인터페이스를 쓴다")
    fun applicationShouldNotBypassDomainInterfaces() {
        noClasses().that().resideInAPackage("com.neki.admin.*.application..")
            .should().dependOnClassesThat().resideInAPackage("..infra..")
            .orShould().dependOnClassesThat().areAssignableTo(Repository::class.java)
            .check(importedClasses)
    }
}
