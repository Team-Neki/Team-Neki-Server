# 오라클 : 사진 공유 링크 (POST /api/shares, GET /share/{token})

발급 응답 `url` 은 공유 웹페이지 주소(`{app.share.web-url}/s/{token}[?{app.share.web-url-query}]`, 카톡 전송용. staging/local 은 `?env=dev`)이고, 이미지 바이너리는 api 의 `GET /share/{token}` 입니다.

이 문서는 사진 공유 링크 작업의 완료 판정 기준을 다룹니다. auto 는 명령이 판정하고, manual 은 배포 후 사람이 확인합니다. 명령은 저장소 루트에서 그대로 실행합니다.

## O-0. 공통

| id | 종류 | 판정 | 명령/절차 |
|---|---|---|---|
| O-0-1 | auto | 포맷 검사 통과 | `./gradlew spotlessCheck -q` 종료코드 0 |
| O-0-2 | auto | 테스트 전부 통과 | `./gradlew test -q` 종료코드 0 |
| O-0-3 | auto | 코드 산출물에 이모지 없음 (macOS BSD grep 은 `-P` 가 없어 perl 로 판정. macOS/Linux 공통) | `git diff origin/main...HEAD -- '*.kt' \| perl -CSD -ne '$c++ if /^\+.*[\x{1F300}-\x{1FAFF}\x{2600}-\x{27BF}]/; END { print $c+0 }'` = 0 |

## O-A. 엔드포인트

| id | 종류 | 판정 |
|---|---|---|
| O-A-1 | auto | 발급 E2E 통과 (소유자만, 재사용, 만료/삭제 링크 시 신규 발급, url = 공유 웹페이지 `https://neki-share.suitestudy.com/s/{token}` (test 프로필은 prod 와 같이 쿼리 없음), D-04, 비인증 403 D-996, 검증 오류) : `./gradlew :apps:api:test --tests 'com.neki.api.e2e.share.IssueShareLinkE2ETest' -q` 종료코드 0 |
| O-A-2 | auto | 조회 E2E 통과 (200 헤더, Security 캐시 헤더 미중복, 404/410, Accept: image/*, HEAD, /file 캐시 헤더 불변) : `./gradlew :apps:api:test --tests 'com.neki.api.e2e.share.ViewSharedImageE2ETest' -q` 종료코드 0 |
| O-A-3 | auto | ArchUnit 통과 (share 등록) : `./gradlew :apps:api:test --tests 'com.neki.api.rule.ArchitectureRulesTest' -q` 종료코드 0 |
| O-A-4 | auto | 단위 테스트 통과 : `./gradlew :domain:test --tests 'com.neki.domain.share.*' -q` 와 `./gradlew :apps:api:test --tests 'com.neki.api.share.*' -q` 종료코드 0 |
| O-A-5 | auto | 마이그레이션은 V48 하나만 추가 : `git diff --name-only origin/main...HEAD -- modules/postgres \| grep -c V48__` = 1 |
| O-A-6 | auto | FileController/FileConverter 미수정 : `git diff --name-only origin/main...HEAD -- apps/api/src/main/kotlin/com/neki/api/media \| wc -l` = 0 |

## O-R. 릴리스

| id | 종류 | 판정 |
|---|---|---|
| O-R-1 | auto | CI 통과 : `gh pr checks feat/photo-share-link` 전부 pass |
| O-R-2 | manual | 배포 순서가 api(V48 적용) -> admin, batch 이고, admin 과 batch 가 Flyway/JPA validate 를 통과해 정상 기동 |
| O-R-3 | manual | staging 에서 발급하면 응답 url 이 `https://neki-share.suitestudy.com/s/{token}?env=dev` 다. 이 url 을 카카오톡과 브라우저에서 열면 웹페이지가 개발 백엔드(`https://dev-yapp.suitestudy.com:4641/share/{token}`)를 호출해 사진이 보인다. 같은 이미지 API 를 직접 `curl -I` 하면 200 이고 `X-Share-Expires-At` 이 `+09:00` 오프셋으로 발급 응답 expiresAt 과 같은 시각이다. prod 발급 url 에는 쿼리가 없다 |
| O-R-4 | manual | staging 에서 공유한 사진을 삭제한 뒤 같은 url(`?env=dev`)을 열면 웹이 만료 안내로 분기하고, 개발 백엔드 `GET /share/{token}` 은 410 이다. 임의 토큰은 404 다 |
| O-R-5 | manual | Loki 에서 `/share` 요청의 `requestUri` 가 `/share/{token}` 으로 마스킹돼 있고 토큰 원문이 없음 |
| O-R-6 | manual | 공유 웹페이지(nginx 같은 도메인 프록시) 경유 호출에서 200/404/410 분기가 동작 |

## 판정 규칙

- auto 하나라도 실패 = 미완료
- manual 은 PR 본문에 "배포 후 미검증" 으로 목록화, 실패 시 티켓 재오픈
