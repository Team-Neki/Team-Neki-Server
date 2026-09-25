# 오라클 : 검색 색인 searchIndexJob (BACKEND-65)

이 문서는 BACKEND-65 작업의 완료 판정 기준을 다룹니다. auto 는 명령이 판정하고, manual 은 배포 후 사람이 확인합니다. 명령은 저장소 루트에서 그대로 실행합니다. 계획은 `docs/superpowers/plans/2026-09-25-search-index-job.md` 입니다.

## 공통 (O-0)

| id | 종류 | 판정 | 명령/절차 |
|---|---|---|---|
| O-0-1 | auto | 포맷 검사 통과 | `./gradlew spotlessCheck -q` 종료코드 0 |
| O-0-2 | auto | 테스트 전부 통과, 모듈별 수 >= baseline (domain 17, apps/batch 7, apps/api 577) | `./gradlew test -q` 종료코드 0 뒤 `for m in domain apps/batch apps/api; do echo "$m $(grep -ho 'tests="[0-9]*"' $m/build/test-results/test/*.xml \| grep -o '[0-9]*' \| paste -sd+ - \| bc) $(grep -ho 'failures="[0-9]*"' $m/build/test-results/test/*.xml \| grep -o '[0-9]*' \| paste -sd+ - \| bc)"; done` |
| O-0-3 | auto | batch bootJar 빌드 성공 | `./gradlew :apps:batch:bootJar -q` 종료코드 0 |
| O-0-4 | auto | 바이너리 오인 파일 없음 | `git diff --stat feat/BACKEND-128...HEAD \| grep -c " Bin "` = 0 |
| O-0-5 | auto | NUL 바이트 없음 | `git diff --name-only feat/BACKEND-128...HEAD \| python3 -c "import sys;bad=[p for p in sys.stdin.read().split() if __import__('os').path.isfile(p) and b'\x00' in open(p,'rb').read()];print(bad);sys.exit(bool(bad))"` 종료코드 0 |
| O-0-6 | auto | 코드 산출물에 이모지 없음 | `git diff feat/BACKEND-128...HEAD -- '*.kt' '*.kts' '*.sql' '*.yaml' '*.yml' \| grep -cP "^\+.*[\x{1F300}-\x{1FAFF}\x{2600}-\x{27BF}]"` = 0 |
| O-0-7 | auto | 도메인 간 import 없음 (search 가 map 의 infra 를 직접 쓰지 않음) | `grep -rn "com.neki.domain.map" domain/src/main/kotlin/com/neki/domain/search \| grep -vc "^$"` = 0 |

## O-A. 스키마, 엔티티, 리포지터리

| id | 종류 | 판정 |
|---|---|---|
| O-A-1 | auto | `ls modules/postgres/src/main/resources/db/migration/V32__add_platform_to_brand.sql modules/postgres/src/main/resources/db/migration/V33__create_photo_booth_search_tables.sql` 종료코드 0 |
| O-A-2 | auto | `grep -c "WHERE deleted_at IS NULL" modules/postgres/src/main/resources/db/migration/V32__add_platform_to_brand.sql` >= 7 (partial unique index 1 + UPDATE 6) |
| O-A-3 | auto | `grep -c "LIFE_FOUR_CUT" modules/postgres/src/main/resources/db/migration/V32__add_platform_to_brand.sql` >= 1 |
| O-A-4 | auto | `grep -cE "ON DELETE CASCADE|USING GIN|USING GIST" modules/postgres/src/main/resources/db/migration/V33__create_photo_booth_search_tables.sql` = 3 |
| O-A-5 | auto | `grep -c 'name = "platform"' domain/src/main/kotlin/com/neki/domain/map/models/Brand.kt` = 1 |
| O-A-6 | auto | 컬럼명 전부 snake_case : `grep -ohE 'name = "[^"]+"' domain/src/main/kotlin/com/neki/domain/search/models/*.kt \| grep -c "[A-Z]"` = 0 |
| O-A-7 | auto | 엔티티에 `unique = true` 없음 : `grep -rc "unique = true" domain/src/main/kotlin/com/neki/domain/search/models/ \| grep -v ":0" \| wc -l` = 0 |
| O-A-8 | auto | `grep -c "@Immutable" domain/src/main/kotlin/com/neki/domain/search/models/PhotoBoothEnriched.kt domain/src/main/kotlin/com/neki/domain/search/models/SubwayStation.kt` 두 파일 모두 1 |
| O-A-9 | auto | H2 에서 새 엔티티 DDL 이 만들어짐 : `./gradlew :apps:batch:test -q` 종료코드 0 |

## O-B. SearchNormalizer

| id | 종류 | 판정 |
|---|---|---|
| O-B-1 | auto | `./gradlew :domain:test --tests 'com.neki.domain.search.SearchNormalizerTest' -q` 종료코드 0 |
| O-B-2 | auto | `grep -cE "fun (normalize|branchName|searchText|regionIds|siteKey)\(" domain/src/main/kotlin/com/neki/domain/search/SearchNormalizer.kt` = 5 |
| O-B-3 | auto | `grep -c "Locale.ROOT" domain/src/main/kotlin/com/neki/domain/search/SearchNormalizer.kt` >= 1 |
| O-B-4 | auto | domain 테스트 수 >= 23 : `./gradlew :domain:test -q; grep -ho 'tests="[0-9]*"' domain/build/test-results/test/*.xml \| grep -o '[0-9]*' \| paste -sd+ - \| bc` |

## O-C. 서비스, 잡, 테스트, 배선

| id | 종류 | 판정 |
|---|---|---|
| O-C-1 | auto | `./gradlew :apps:batch:test -q` 종료코드 0, batch 테스트 수 >= 13 : `grep -ho 'tests="[0-9]*"' apps/batch/build/test-results/test/*.xml \| grep -o '[0-9]*' \| paste -sd+ - \| bc` |
| O-C-2 | auto | `grep -rn sampleJob --exclude-dir=build --exclude-dir=.git --exclude-dir=docs --exclude-dir=.worktrees . \| wc -l` = 0 |
| O-C-3 | auto | `grep -c '"com.neki.domain.search"' apps/batch/src/main/kotlin/com/neki/batch/NekiBatchApplication.kt` = 1 |
| O-C-4 | auto | `grep -c "@Transactional" domain/src/main/kotlin/com/neki/domain/search/service/SearchIndexService.kt` = 1 |
| O-C-5 | auto | `grep -c "RunIdIncrementer()" apps/batch/src/main/kotlin/com/neki/batch/search/SearchIndexJobConfig.kt` = 1 |
| O-C-6 | auto | `grep -c "distanceTo" domain/src/main/kotlin/com/neki/domain/search/service/SearchIndexService.kt` >= 1 (기존 haversine 재사용) |
| O-C-7 | auto | `grep -c "ponytail:" domain/src/main/kotlin/com/neki/domain/search/service/SearchIndexService.kt` >= 1 (전수 비교 상한 표시) |
| O-C-8 | auto | `test ! -e apps/batch/src/main/kotlin/com/neki/batch/sample/SampleJobConfig.kt` 종료코드 0 |
| O-C-9 | auto | `grep -c "searchIndexJob" .claude/CLAUDE.md README.md apps/batch/src/main/resources/application.yaml` 세 파일 모두 >= 1 |

## O-R. 릴리스

| id | 종류 | 판정 |
|---|---|---|
| O-R-1 | auto | `gh pr view feat/BACKEND-65 --json state,baseRefName --jq '.state + " " + .baseRefName'` = `OPEN feat/BACKEND-128` (머지 전) |
| O-R-2 | auto | CI 통과 : `gh pr checks feat/BACKEND-65 --json name,state --jq '.[] \| select(.name=="Test") \| .state'` = `SUCCESS` |
| O-R-3 | manual | api 배포로 V32, V33 적용 뒤 `deploy-batch.yml` 로 이미지 push, Prefect `search-index` flow 수동 실행 -> k8s Job 종료 코드 0, `select count(*) from tb_photo_booth_search` > 0, `select platform, count(*) from tb_photo_booth_search group by 1` 에 platform 이 채워진 브랜드가 전부 보임 |
| O-R-4 | manual | 같은 `businessDate` 로 flow 를 한 번 더 실행 -> 두 테이블 건수 동일, `BATCH_JOB_INSTANCE` 에 인스턴스 하나 추가 |
| O-R-5 | manual | `update tb_brand set platform = ... where code = ...` 로 나머지 5개 브랜드 채운 뒤 재실행 -> 그 브랜드 카드가 생김 |
| O-R-6 | auto | 티켓 BACKEND-65 DONE (배포 뒤) |

## 판정 규칙

- auto 하나라도 실패 = 미완료
- manual 은 PR 본문에 "배포 후 미검증" 으로 목록화, 실패 시 티켓 재오픈
- auto 전부 통과 + PR 생성 + CI 통과 = 에이전트 측 완료. manual 통과 = 기능 완료
