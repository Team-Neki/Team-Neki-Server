# 어드민 앱 요청 flow

이 문서는 `apps/admin` 앱의 공통 구성과 기능을 추가할 때 따르는 요청 처리 구조를 다룹니다.

## 모듈 구성

어드민 앱은 `:domain`의 도메인 서비스를 재사용하는 실행 모듈입니다. `apps:api`를 의존하지 않고 API 계층과 인프라 어댑터를 자체로 소유합니다.

컴포넌트 스캔은 `com.neki.admin` + `com.neki.core` + `com.neki.config.postgres` + `com.neki.config.jasypt`로 한정합니다. `:domain` 전체를 스캔하면 사용하지 않는 도메인 서비스와 그 서비스의 인프라 의존성까지 요구하기 때문입니다. 도메인 서비스는 각 기능의 설정 클래스에서 명시적으로 등록합니다.

공통 구성은 다음과 같습니다.

- `AdminApplication`: 컴포넌트·엔티티·JPA 리포지터리 스캔 범위 지정
- `modules:postgres`의 `QueryDslConfig`와 `JpaAuditingConfig`: QueryDSL과 JPA Auditing 공통 설정 재사용
- `modules:postgres`의 `application-postgres.yaml`: API와 동일한 PostgreSQL 연결·JPA·Flyway 설정 로드
- `modules:jasypt`: API와 동일한 `JASYPT_PASSWORD`로 staging/prod의 암호화된 연결 정보 복호화
- `ExceptionHandler`: `apps:api`와 같은 `ExceptionMsg` 응답 형식으로 예외 처리
- `AdminMediaVerifier`: LOGO/POSE media의 존재·타입·상태를 확인하고 S3 객체가 있으면 UPLOADED로 전환
- `local`/`staging`/`prod` 프로파일: API와 같은 DB·스키마의 기존 테이블 사용, 8081 포트
- `test` 프로파일: admin 테스트 설정의 H2로 인증·세션 컨텍스트 검증

## 공유 PostgreSQL 사용

admin은 API와 동일한 접속 정보와 도메인 엔티티를 사용합니다. 별도 스키마나 `currentSchema`·`default_schema`를 지정하지 않으므로 API와 같은 기본 스키마의 `TB_BRAND`, `TB_POSE` 등 기존 테이블을 조회·수정합니다.

테이블 생성·변경은 API가 기존 `modules/postgres`의 Flyway 마이그레이션으로 수행합니다. admin의 `FlywayMigrationStrategy`는 `validate()`만 호출하므로 마이그레이션이나 이력 기록을 수행하지 않습니다. 새 마이그레이션이 있으면 API를 먼저 배포해야 하며, 초기 로컬 DB도 API로 먼저 준비합니다.

DB 설정은 `modules/postgres/src/main/resources`에서 관리합니다. admin의 메인·테스트 YAML은 필요한 파일을 `spring.config.import`로 가져옵니다. `local`/`staging`/`prod` 실행에는 기존 API와 동일한 `JASYPT_PASSWORD`를 제공합니다. H2 드라이버와 메모리 DB 설정은 테스트에서만 사용합니다.

## 요청 처리 구조

```text
Request -> Controller -> Mapper(toCommand/toQuery) -> Facade -> 도메인 Service -> Repository 어댑터 -> JPA/QueryDSL
```

- Controller: 요청 검증(`@Valid`)과 `BaseResponse` 래핑
- Mapper: `XxxAdminDto.Request`를 `Command`/`Query`로 변환하는 top-level 확장 함수
- Facade: 트랜잭션 경계를 잡고 도메인 서비스에 위임
- 도메인 Service: `:domain`의 서비스를 재사용

| 항목 | apps:api | apps:admin |
| --- | --- | --- |
| application 계층 | `*UseCase` (기능 단위 클래스) | `*Facade` (도메인 단위 클래스) |
| DTO | `Request`/`Response`/`Converter` 분리 | `XxxAdminDto` object 하위 중첩 + Mapper 확장 함수 |
| 인증 | JWT (`@AuthenticationPrincipal`) | 소셜 OAuth2 로그인·JDBC 세션, 요청마다 현재 DB ADMIN 권한 확인 |
| media 검증 | `MediaClient.verifyMediasUploaded` 후 보상 롤백 | 공유 트랜잭션에서 S3 존재 확인·상태 전환 |

admin은 교차 도메인 오케스트레이션이 없어 도메인당 Facade 하나에 조회·등록·수정·삭제를 위임합니다. 기능이 늘어나면 Facade가 비대해질 수 있습니다.

도메인 리포지터리 인터페이스는 `:domain`이 소유하고 admin용 구현 어댑터는 각 기능의 `infra/persist`에 둡니다. 사용자 컨텍스트가 필요한 API 전용 메서드는 `UnsupportedOperationException`으로 지원 범위를 표시합니다. 호출 경로가 없다는 사실이 컴파일 타임에 보장되지는 않습니다.

목록 조회는 `CountedPage`(전체 건수 + 전체 페이지 수)를 사용합니다. 무한 스크롤용 `Page`(hasNext)와 달리 관리자 화면의 페이지네이션 UI를 전제로 합니다.

## 현재 제한

Spring Security가 `/admin/v1/**` 요청을 인증·인가하고 변경 요청에 CSRF를 검사합니다. 임시 `AdminWriteGuard`는 제거했습니다. 관리자 업로드 티켓은 업로더의 ID를 `TB_MEDIA.owner_id`에 기록하고 공동 관리 대상으로 표시합니다. Brand/Pose는 이 표시가 있는 미디어만 연결하며, 현재 ADMIN 권한이 있는 다른 관리자도 등록·수정할 수 있습니다.
