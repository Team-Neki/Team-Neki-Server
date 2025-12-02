## 🔧  프로젝트 설정

### Java/Kotlin 환경
```kotlin
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```

- **Java Version**: OpenJDK 21 (Long-Term Support)
- **Kotlin Version**: 2.0.10 (최신 안정 버전)
- **Spring Boot**: 3.5.8

---

## ⚙️ Kotlin Compiler 설정

```kotlin
tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.addAll(listOf("-Xjsr305=strict"))
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
```

### 설정 상세
- **-Xjsr305=strict**: Null-safety 검사 강제 [링크](https://peterica.tistory.com/821)
- **jvmTarget**: Java 21 바이트코드 생성

---

## 📝 Plugin 구성

```kotlin
plugins {
    val kotlinVersion = "2.0.10"

    id("org.springframework.boot") version "3.5.8"
    id("io.spring.dependency-management") version "1.1.6"

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    idea
}
```

### Plugin 역할
- **spring.boot**: 부트 애플리케이션 패키징 및 빌드
- **dependency-management**: Maven BOM 기반 버전 관리
- **kotlin.jvm**: Kotlin/JVM 컴파일
- **kotlin.spring**: Spring 플러그인 (all-open 자동 적용)
- **kotlin.jpa**: JPA 엔티티 생성자 자동화
- **kapt**: Kotlin Annotation Processing Tool
- **idea**: IntelliJ IDEA 프로젝트 생성

---