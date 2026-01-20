# E2E 테스트 문제 해결 가이드

## 문제 증상

### 발생한 현상
- **개별 테스트 실행**: ✅ 성공
- **전체 테스트 실행**: ❌ `DeleteFolderE2ETest.givenExistingFolder_whenDeleteFolder_thenReturnsSuccess` 실패

```
java.lang.AssertionError: 1 expectation failed.
Expected status code <200> but was <400>.
```

### 테스트 환경
- Spring Boot Test with `RANDOM_PORT`
- H2 In-Memory Database (`testdb`)
- JPA `ddl-auto: create-drop`
- E2E 테스트: RestAssured를 통한 실제 HTTP 호출

---

## 문제 분석 과정

### 1단계: 테스트 격리 문제 의심

**가설**: 테스트 간 데이터가 공유되어 간섭이 발생하는가?

**검증**:
```kotlin
// FolderE2ETestBase.kt
@AfterEach
override fun tearDown() {
    folderRepository.deleteAllInBatch()  // ✅ 폴더 정리
    super.tearDown()                      // ✅ 사용자 정리
}
```

**결과**: `DeleteFolderE2ETest`와 `CreateFolderE2ETest`는 모두 `FolderE2ETestBase`를 상속하여 cleanup 정상 실행

### 2단계: 다른 테스트 클래스 확인

**검증**:
```kotlin
// GetAllFolderE2ETest.kt - Line 28
class GetAllFolderE2ETest : E2ETestBase() {  // ⚠️ FolderE2ETestBase가 아님!
```

**발견**: `GetAllFolderE2ETest`만 `E2ETestBase`를 직접 상속
- `@AfterEach`에서 `userRepository.deleteAllInBatch()`만 실행
- **폴더 cleanup 누락!** ❌

### 3단계: 테스트 실행 순서 영향 분석

**시나리오**:
```
1. GetAllFolderE2ETest 실행
   ├─ givenNoFolders... ✅
   ├─ givenExistingFolders... (폴더 4개 생성) ✅
   ├─ givenSingleFolder... (폴더 1개 생성) ✅
   └─ @AfterEach: userRepository만 삭제
      → ⚠️ 폴더 5개가 DB에 잔류
      → ⚠️ User 삭제 시도 → FK 제약조건 위반 가능

2. DeleteFolderE2ETest 실행
   ├─ @BeforeEach: 새 user 생성
   └─ givenExistingFolder_whenDeleteFolder_thenReturnsSuccess
      ├─ 폴더 생성 API 호출
      ├─ 폴더 삭제 API 호출
      └─ ❌ 400 에러: 폴더를 찾을 수 없음
```

### 4단계: API 레이어 분석

**DeleteFolderUseCase.kt - Line 23**:
```kotlin
val folder = folderRepository.getOwnedFolder(command.userId, command.folderId)
    ?: throw BusinessException(ResultCode.NOT_FOUND)  // ← 400 에러 발생 지점
```

**의문점**: 폴더를 방금 생성했는데 왜 조회가 안 될까?

### 5단계: Repository 메서드 분석

**JpaFolderRepository.kt - Line 14**:
```kotlin
fun findByIdAndUserId(userId: Long, folderId: Long): Folder?
```

**문제 발견!** 🎯

Spring Data JPA 메서드 네이밍 규칙:
- `findByIdAndUserId` = `findBy [Id] And [UserId]`
- 첫 번째 파라미터: `id`
- 두 번째 파라미터: `userId`

**현재 코드**:
```kotlin
// 메서드명: findByIdAndUserId
// 파라미터: (userId: Long, folderId: Long)
// 실제 매핑:
//   - id = userId (잘못된 값!)
//   - userId = folderId (잘못된 값!)
```

**다른 메서드들과 비교**:
```kotlin
fun findAllByUserId(userId: Long): List<Folder>                    // ✅ userId 먼저
fun findAllByUserIdAndIdIn(userId: Long, folderIds: List<Long>)   // ✅ userId 먼저
fun deleteByUserIdAndId(userId: Long, folderId: Long)             // ✅ userId 먼저
```

**일관성 부족**: 모든 메서드는 `userId`가 먼저인데, `findByIdAndUserId`만 메서드명이 다름

---

## 근본 원인

### 원인 1: GetAllFolderE2ETest의 잘못된 상속 구조

```kotlin
// ❌ 잘못된 코드
class GetAllFolderE2ETest : E2ETestBase() {
    @Autowired private lateinit var folderRepository: JpaFolderRepository
    // folderRepository cleanup 없음
}

// ✅ 올바른 코드
class GetAllFolderE2ETest : FolderE2ETestBase() {
    // folderRepository는 부모에서 제공
    // @AfterEach cleanup 자동 실행
}
```

**영향**:
- 테스트 간 데이터 격리 실패
- Foreign Key 제약조건 위반 가능성
- 예측 불가능한 테스트 실패

### 원인 2: Spring Data JPA 메서드명과 파라미터 불일치

```kotlin
// ❌ 잘못된 코드
fun findByIdAndUserId(userId: Long, folderId: Long): Folder?
// Spring Data JPA 해석:
//   WHERE id = :userId AND userId = :folderId  (완전히 반대!)

// ✅ 올바른 코드
fun findByUserIdAndId(userId: Long, folderId: Long): Folder?
// Spring Data JPA 해석:
//   WHERE userId = :userId AND id = :folderId  (정확!)
```

**영향**:
- 잘못된 파라미터 매핑으로 조회 실패
- 존재하는 폴더를 찾지 못함
- 400 NOT_FOUND 에러 발생

---

## 해결 방법

### 수정 1: GetAllFolderE2ETest 상속 구조 변경

**파일**: `src/test/kotlin/com/yapp2app/e2e/photo/folder/GetAllFolderE2ETest.kt`

```diff
- import com.yapp2app.e2e.E2ETestBase
- import com.yapp2app.photo.infra.persist.jpa.JpaFolderRepository
- import org.springframework.beans.factory.annotation.Autowired

- class GetAllFolderE2ETest : E2ETestBase() {
+ class GetAllFolderE2ETest : FolderE2ETestBase() {

-     @Autowired private lateinit var folderRepository: JpaFolderRepository
+     // folderRepository는 FolderE2ETestBase에서 protected로 제공됨
```

**효과**:
- `@AfterEach`에서 `folderRepository.deleteAllInBatch()` 자동 실행
- 테스트 간 데이터 격리 보장
- Foreign Key 제약조건 정상 처리

### 수정 2: JpaFolderRepository 메서드명 수정

**파일**: `src/main/kotlin/com/yapp2app/photo/infra/persist/jpa/JpaFolderRepository.kt`

```diff
- fun findByIdAndUserId(userId: Long, folderId: Long): Folder?
+ fun findByUserIdAndId(userId: Long, folderId: Long): Folder?
```

**파일**: `src/main/kotlin/com/yapp2app/photo/infra/persist/FolderRepositoryAdapter.kt`

```diff
  override fun getOwnedFolder(userId: Long, folderId: Long) =
-     jpaRepository.findByIdAndUserId(userId, folderId)
+     jpaRepository.findByUserIdAndId(userId, folderId)
```

**효과**:
- Spring Data JPA 메서드 네이밍 규칙 준수
- 파라미터 매핑 정확성 보장
- 다른 메서드들과 일관성 유지

---

## 검증 결과

### 개별 테스트
```bash
./gradlew test --tests "DeleteFolderE2ETest"
./gradlew test --tests "GetAllFolderE2ETest"
./gradlew test --tests "CreateFolderE2ETest"
```
**결과**: ✅ 모두 성공

### 전체 테스트
```bash
./gradlew test --tests "com.yapp2app.e2e.photo.folder.*"
```
**결과**: ✅ BUILD SUCCESSFUL (총 23개 테스트)

---

## 예방 방법

### 1. E2E 테스트 Base Class 사용 원칙

**원칙**: 도메인별 Base Class를 반드시 상속하라

```kotlin
// ✅ 올바른 패턴
abstract class FolderE2ETestBase : E2ETestBase() {
    @Autowired
    protected lateinit var folderRepository: JpaFolderRepository

    @AfterEach
    override fun tearDown() {
        folderRepository.deleteAllInBatch()  // 도메인 데이터 정리
        super.tearDown()                      // 공통 데이터 정리
    }
}

class AnyFolderE2ETest : FolderE2ETestBase() {
    // folderRepository cleanup 자동 보장
}
```

### 2. Spring Data JPA 메서드 네이밍 검증

**체크리스트**:
- [ ] 메서드명이 Spring Data JPA 규칙을 따르는가?
- [ ] 파라미터 순서가 메서드명의 속성 순서와 일치하는가?
- [ ] 같은 Repository의 다른 메서드들과 일관성이 있는가?

**예시**:
```kotlin
// ✅ 올바른 패턴
fun findByUserIdAndName(userId: Long, name: String)
fun findByUserIdAndId(userId: Long, id: Long)
fun findAllByUserIdAndIdIn(userId: Long, ids: List<Long>)
fun deleteByUserIdAndId(userId: Long, id: Long)

// ❌ 잘못된 패턴 (파라미터 순서와 메서드명 불일치)
fun findByIdAndUserId(userId: Long, id: Long)  // 순서 반대!
```

### 3. 테스트 격리 검증

**전체 테스트 실행 습관화**:
```bash
# 개별 테스트만 통과해도 만족하지 말 것
./gradlew test --tests "SpecificTest"  # ❌ 불충분

# 항상 전체 테스트 실행으로 검증
./gradlew test --tests "com.yapp2app.e2e.photo.folder.*"  # ✅ 권장
```

### 4. Code Review 체크포인트

**E2E 테스트 작성 시 확인 사항**:
1. [ ] 적절한 Base Class를 상속했는가?
2. [ ] `@AfterEach`에서 생성한 데이터를 모두 정리하는가?
3. [ ] Foreign Key 관계를 고려한 삭제 순서인가?
4. [ ] Repository 메서드명과 파라미터가 일치하는가?
5. [ ] 전체 테스트 실행 시 통과하는가?

---

## 참고 자료

### Spring Data JPA Query Methods
- [Spring Data JPA Reference Documentation](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)
- Query Method Naming: `findBy[Property1]And[Property2](param1, param2)`
- 파라미터 순서는 메서드명의 속성 순서와 반드시 일치해야 함

### Spring Boot Test
- `@SpringBootTest(webEnvironment = RANDOM_PORT)`: 실제 서버 구동
- 각 HTTP 요청은 별도 트랜잭션에서 실행됨
- 테스트 메서드는 기본적으로 트랜잭션이 아님 (별도 `@Transactional` 필요)

### H2 Database
- `DB_CLOSE_DELAY=-1`: 마지막 연결 종료 시에도 DB 유지
- `ddl-auto: create-drop`: 테스트 클래스 단위로 스키마 재생성
- 같은 테스트 클래스 내 메서드들은 DB 공유

---

## 작성 정보
- **작성일**: 2025-12-29
- **작성자**: koo
- **관련 이슈**: E2E 테스트 격리 문제 및 Repository 메서드 파라미터 불일치
