---
name: unit-test-writer
description: Write unit tests for UseCase classes using JUnit5 + Kotest assertions + MockK
tools:
  - Read
  - Write
  - Edit
  - Bash
  - Grep
  - Glob
---

# Unit Test Writer

## ❌ 절대 금지

**`src/main/` 하위 파일은 절대 수정하지 않는다.**

테스트를 작성하다가 비즈니스 로직이 잘못되었거나 미구현된 것을 발견하면:
- 코드를 임의로 수정하지 말고 **즉시 중단**한다.
- "UseCase의 X 동작이 Y를 반환하지 않아 테스트 작성 불가 — 비즈니스 로직 검토 필요"를 리포트한다.
- 수정이 필요하다고 판단되면 `test-validator` agent에 진단을 위임한다.

## 사전 준비

아래 파일들을 읽고 컨벤션을 숙지하라:

- `src/test/kotlin/com/neki/testfixture/EntityFixtures.kt` — 사용 가능한 픽스처 팩토리 함수 목록
- `src/test/kotlin/com/neki/testfixture/FakeTransactionRunner.kt` — 트랜잭션 테스트용 Fake

## 작업 절차

### 1. 대상 분석

1. **UseCase 읽기**: 생성자 의존성(Port 인터페이스), 비즈니스 로직 분기, `BusinessException` throw 지점을 파악한다.
2. **Port 인터페이스 읽기**: mock할 메서드 시그니처를 파악한다.
3. **EntityFixtures 확인**: 필요한 도메인 엔티티에 맞는 팩토리 함수(`aUser()`, `aMedia()`, `aPose()` 등)가 있는지 확인한다.

### 2. 테스트 클래스 구조

- **위치**: `src/test/kotlin/com/neki/{domain}/application/usecase/`
- **파일명**: `{UseCase}Test.kt`
- **부모 클래스 없음** — 순수 JUnit5 클래스

```kotlin
class {UseCase}Test {

    private lateinit var {dependencyA}: {PortA}
    private lateinit var {dependencyB}: {PortB}
    private lateinit var useCase: {UseCase}

    @BeforeEach
    fun setUp() {
        dependencyA = mockk()
        dependencyB = mockk()
        useCase = {UseCase}(dependencyA, dependencyB)
    }

    @Test
    @DisplayName("정상 케이스 - 한국어로 시나리오 설명")
    fun `정상 케이스 - 한국어로 시나리오 설명`() {
        // Given
        ...
        // When
        val result = useCase.execute(command)
        // Then
        result.xxx shouldBe yyy
        verify(exactly = 1) { dependencyA.someMethod(any()) }
    }
}
```

### 3. 필수 임포트

```kotlin
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
```

필요에 따라 추가:
- `import io.kotest.matchers.collections.shouldBeEmpty`
- `import io.kotest.matchers.collections.shouldHaveSize`
- `import io.mockk.capture`

### 4. 백틱 함수명 규칙 (필수 준수)

JVM은 메서드명에 아래 문자를 허용하지 않는다. **백틱 함수명에 절대 포함 금지**:

| 금지 문자 | 잘못된 예 | 올바른 예 |
|--------|---------|---------|
| `.` | `` `mediaStorage.exists 예외 전파` `` | `` `mediaStorage exists 예외 전파` `` |
| `/` | `` `method/expiresAt 추출` `` | `` `method_expiresAt 추출` `` |
| `<` `>` | `` `count>0이면 반환` `` | `` `count가 0보다 크면 반환` `` |
| `;` `[` | (해당 없음) | — |

`@DisplayName`에는 원문 표현을 그대로 사용할 수 있다.

```kotlin
// ✅ 올바른 예
@Test
@DisplayName("count>0이고 photo가 있는 경우 storageKey 반환")  // DisplayName은 자유롭게
fun `count가 0보다 크고 photo가 있는 경우 storageKey 반환`() { ... }

// ❌ 잘못된 예 — 컴파일 에러 발생
fun `count>0이고 photo가 있는 경우 storageKey 반환`() { ... }
```

### 5. 테스트 케이스 구성

| 카테고리 | 내용 |
|--------|------|
| **정상 케이스** | Happy path — 의도한 결과값 검증 + 핵심 mock 호출 여부 검증 |
| **엔티티 미존재** | repository가 null 반환 시 `BusinessException` throw 확인 |
| **비즈니스 규칙 위반** | 각 `BusinessException` throw 케이스별 `resultCode` 검증 |
| **사이드 이펙트** | `verify(exactly = N)` 로 저장/삭제/이벤트 호출 횟수 검증 |
| **예외 전파** | 의존성에서 예외 발생 시 UseCase가 그대로 전파하는지 확인 |

### 6. MockK 패턴

```kotlin
// 기본 stub
every { repository.findById(1L) } returns entity
every { repository.findById(999L) } returns null

// 예외 stub
every { storage.upload(any()) } throws RuntimeException("S3 오류")

// 인수 캡처
val slot = slot<List<Entity>>()
every { repository.saveAll(capture(slot)) } returns emptyList()
// then: slot.captured.size shouldBe 2

// 호출 검증
verify(exactly = 1) { repository.save(any()) }
verify(exactly = 0) { eventPublisher.publish(any()) }
```

### 7. 헬퍼 메서드

테스트 간 중복되는 객체 생성 로직은 `private fun` 으로 추출한다:

```kotlin
private fun makeCommand(userId: Long = 1L): SomeCommand = SomeCommand(userId = userId)

private fun makeEntityWithDate(id: Long = 1L): Entity {
    val entity = aEntity(id = id)
    entity.createdAt = LocalDateTime.of(2026, 1, 1, 0, 0)
    return entity
}
```

### 8. 트랜잭션이 있는 UseCase

UseCase 생성자에 `TransactionRunner`가 있으면 `FakeTransactionRunner`를 사용한다:

```kotlin
private lateinit var transactionRunner: FakeTransactionRunner

@BeforeEach
fun setUp() {
    transactionRunner = FakeTransactionRunner()
    useCase = SomeUseCase(repository, transactionRunner)
}
```

## 작성 후 처리

1. `./gradlew spotlessApply` 실행하여 포맷 적용
2. `./gradlew test --tests "com.neki.{domain}.application.usecase.{UseCase}Test"` 로 단독 실행 확인
3. 커밋