# 어드민 앱 요청 flow

이 문서는 `apps/admin` 앱의 공통 구성과 기능을 추가할 때 따르는 요청 처리 구조를 다룹니다.

## 모듈 구성

어드민 앱은 `:domain`의 도메인 서비스를 재사용하는 실행 모듈입니다. `apps:api`를 의존하지 않고 API 계층과 인프라 어댑터를 자체로 소유합니다.

컴포넌트 스캔은 `com.neki.admin` + `com.neki.core`로 한정합니다. `:domain` 전체를 스캔하면 사용하지 않는 도메인 서비스와 그 서비스의 인프라 의존성까지 요구하기 때문입니다. 도메인 서비스는 각 기능의 설정 클래스에서 명시적으로 등록합니다.

공통 구성은 다음과 같습니다.

- `AdminApplication`: 컴포넌트·엔티티·JPA 리포지터리 스캔 범위 지정
- `QueryDslConfig`: QueryDSL과 JPA Auditing 구성
- `ExceptionHandler`: `apps:api`와 같은 `ExceptionMsg` 응답 형식으로 예외 처리
- `local` 프로파일: 8081 포트, 메모리 H2, Flyway 비활성화
- `test` 프로파일: H2 기반 애플리케이션 컨텍스트 검증

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
| 인증 | JWT (`@AuthenticationPrincipal`) | 미구현 |
| media 검증 | `MediaClient.verifyMediasUploaded` 후 보상 롤백 | 미구현 |

admin은 교차 도메인 오케스트레이션이 없어 도메인당 Facade 하나에 조회·등록·수정·삭제를 위임합니다. 기능이 늘어나면 Facade가 비대해질 수 있습니다.

도메인 리포지터리 인터페이스는 `:domain`이 소유하고 admin용 구현 어댑터는 각 기능의 `infra/persist`에 둡니다. 사용자 컨텍스트가 필요한 API 전용 메서드는 `UnsupportedOperationException`으로 지원 범위를 표시합니다. 호출 경로가 없다는 사실이 컴파일 타임에 보장되지는 않습니다.

목록 조회는 `CountedPage`(전체 건수 + 전체 페이지 수)를 사용합니다. 무한 스크롤용 `Page`(hasNext)와 달리 관리자 화면의 페이지네이션 UI를 전제로 합니다.

## 현재 제한

인증 도입 전까지 Spring Security 자동설정을 제외합니다. 외부 노출 전 인증이 필요하며 계획은 `docs/superpowers/plans/2026-08-16-admin-auth.md`에 있습니다.

메모리 H2 구성은 재시작 시 데이터가 초기화됩니다. 운영 PostgreSQL 연결과 프로파일 분리는 후속 작업입니다.
