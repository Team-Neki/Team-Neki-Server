# 오라클 : 검색 색인 searchIndexJob (BACKEND-65)

이 문서는 BACKEND-65 작업의 완료 판정 기준을 다룹니다. auto 는 명령이 판정하고, manual 은 배포 후 사람이 확인합니다. 명령은 저장소 루트에서 그대로 실행합니다. 계획은 `docs/superpowers/plans/2026-09-25-search-index-job.md` 입니다.

## 공통 (O-0)

| id | 종류 | 판정 | 명령/절차 |
|---|---|---|---|
| O-0-1 | auto | 포맷 검사 통과 | `./gradlew spotlessCheck -q` 종료코드 0 |
| O-0-2 | auto | 테스트 전부 통과, 모듈별 수 >= baseline (domain 17, apps/batch 7, apps/api 577) | `./gradlew test -q` 종료코드 0 뒤 `for m in domain apps/batch apps/api; do echo "$m $(grep -ho 'tests="[0-9]*"' $m/build/test-results/test/*.xml \| grep -o '[0-9]*' \| paste -sd+ - \| bc) $(grep -ho 'failures="[0-9]*"' $m/build/test-results/test/*.xml \| grep -o '[0-9]*' \| paste -sd+ - \| bc)"; done` |
| O-0-3 | auto | batch bootJar 빌드 성공 | `./gradlew :apps:batch:bootJar -q` 종료코드 0 |
| O-0-4 | auto | 바이너리 오인 파일 없음 | `git diff --stat origin/main...HEAD \| grep -c " Bin "` = 0 |
| O-0-5 | auto | NUL 바이트 없음 | `git diff --name-only origin/main...HEAD \| python3 -c "import sys;bad=[p for p in sys.stdin.read().split() if __import__('os').path.isfile(p) and b'\x00' in open(p,'rb').read()];print(bad);sys.exit(bool(bad))"` 종료코드 0 |
| O-0-6 | auto | 코드 산출물에 이모지 없음 | `git diff origin/main...HEAD -- '*.kt' '*.kts' '*.sql' '*.yaml' '*.yml' \| grep -cP "^\+.*[\x{1F300}-\x{1FAFF}\x{2600}-\x{27BF}]"` = 0 |
| O-0-7 | auto | 도메인 간 import 없음 (search 가 map 의 infra 를 직접 쓰지 않음) | `grep -rn "com.neki.domain.map" domain/src/main/kotlin/com/neki/domain/search \| grep -vc "^$"` = 0 |

## O-A. 스키마, 엔티티, 리포지터리

| id | 종류 | 판정 |
|---|---|---|
| O-A-1 | auto | `ls modules/postgres/src/main/resources/db/migration/V32__add_platform_to_brand.sql modules/postgres/src/main/resources/db/migration/V33__create_photo_booth_search_tables.sql` 종료코드 0 |
| O-A-2 | auto | `grep -c "WHERE deleted_at IS NULL" modules/postgres/src/main/resources/db/migration/V32__add_platform_to_brand.sql` >= 7 (partial unique index 1 + UPDATE 6) |
| O-A-3 | auto | `grep -c "LIFE_FOUR_CUT" modules/postgres/src/main/resources/db/migration/V32__add_platform_to_brand.sql` >= 1 |
| O-A-4 | auto | `grep -cE "ON DELETE CASCADE\|USING GIN\|USING GIST" modules/postgres/src/main/resources/db/migration/V33__create_photo_booth_search_tables.sql` = 6 (두 벌) |
| O-A-10 | auto | 카드·연결 테이블이 read/write 두 벌 : `grep -c "^CREATE TABLE tb_photo_booth_search_" modules/postgres/src/main/resources/db/migration/V33__create_photo_booth_search_tables.sql` = 4 |
| O-A-5 | auto | `grep -c 'name = "platform"' domain/src/main/kotlin/com/neki/domain/map/models/Brand.kt` = 1 |
| O-A-6 | auto | 컬럼명 전부 snake_case : `grep -ohE 'name = "[^"]+"' domain/src/main/kotlin/com/neki/domain/search/models/*.kt \| grep -c "[A-Z]"` = 0 |
| O-A-7 | auto | 엔티티에 `unique = true` 없음 : `grep -rc "unique = true" domain/src/main/kotlin/com/neki/domain/search/models/ \| grep -v ":0" \| wc -l` = 0 |
| O-A-8 | auto | `grep -c "@Immutable" domain/src/main/kotlin/com/neki/domain/search/models/PhotoBoothEnriched.kt domain/src/main/kotlin/com/neki/domain/search/models/SubwayStation.kt` 두 파일 모두 1 |
| O-A-9 | auto | H2 에서 새 엔티티 DDL 이 만들어짐 : `./gradlew :apps:batch:test -q` 종료코드 0 |

## O-B. SearchNormalizer

| id | 종류 | 판정 |
|---|---|---|
| O-B-1 | auto | `./gradlew :domain:test --tests 'com.neki.domain.search.SearchNormalizerTest' -q` 종료코드 0 |
| O-B-2 | auto | `grep -cE "fun (normalize\|branchName\|searchText\|regionIds\|siteKey)\(" domain/src/main/kotlin/com/neki/domain/search/service/qu/SearchNormalizer.kt` = 5 |
| O-B-3 | auto | `grep -c "Locale.ROOT" domain/src/main/kotlin/com/neki/domain/search/service/qu/SearchNormalizer.kt` >= 1 |
| O-B-4 | auto | domain 테스트 수 >= 23 : `./gradlew :domain:test -q; grep -ho 'tests="[0-9]*"' domain/build/test-results/test/*.xml \| grep -o '[0-9]*' \| paste -sd+ - \| bc` |

## O-C. 서비스, 잡, 테스트, 배선

| id | 종류 | 판정 |
|---|---|---|
| O-C-1 | auto | `./gradlew :apps:batch:test -q` 종료코드 0, batch 테스트 수 >= 13 : `grep -ho 'tests="[0-9]*"' apps/batch/build/test-results/test/*.xml \| grep -o '[0-9]*' \| paste -sd+ - \| bc` |
| O-C-2 | auto | `grep -rn sampleJob --exclude-dir=build --exclude-dir=.git --exclude-dir=docs --exclude-dir=.worktrees . \| wc -l` = 0 |
| O-C-3 | auto | `grep -c '"com.neki.domain.search"' apps/batch/src/main/kotlin/com/neki/batch/bootstrap/ComponentScanConfig.kt` = 1 |
| O-C-4 | auto | 트랜잭션은 TaskletStep 에 맡긴다 : `grep -rc "@Transactional" apps/batch/src/main \| grep -v ":0" \| wc -l` = 0, `test ! -e apps/batch/src/main/kotlin/com/neki/batch/search/application` 종료코드 0 |
| O-C-5 | auto | `grep -c "RunIdIncrementer()" apps/batch/src/main/kotlin/com/neki/batch/search/job/SearchIndexJob.kt` = 1 |
| O-C-6 | auto | `grep -c "distanceTo" domain/src/main/kotlin/com/neki/domain/search/models/SubwayStation.kt` >= 1 (기존 haversine 재사용, 도메인이 계산) |
| O-C-7 | auto | `grep -c "ponytail:" domain/src/main/kotlin/com/neki/domain/search/models/NearbyStation.kt` >= 1 (전수 비교 상한 표시) |
| O-C-8 | auto | `test ! -e apps/batch/src/main/kotlin/com/neki/batch/sample/SampleJobConfig.kt` 종료코드 0 |
| O-C-9 | auto | `grep -c "searchIndexJob" .claude/CLAUDE.md README.md apps/batch/src/main/resources/application.yaml` 세 파일 모두 >= 1 |
| O-C-10 | auto | swap 은 _tmp 를 거치는 회전 : `grep -c "RENAME TO" domain/src/main/kotlin/com/neki/domain/search/infra/persist/PhotoBoothSearchRepositoryAdapter.kt` = 3 |
| O-C-11 | auto | 파생 필드는 도메인 팩토리만 만든다 : `grep -c "private constructor" domain/src/main/kotlin/com/neki/domain/search/models/PhotoBoothSearchWrite.kt` = 1, `grep -rc "SearchNormalizer" apps/batch/src/main \| grep -v ":0" \| wc -l` = 0 |
| O-C-12 | auto | 두 step : `grep -cE "BUILD_STEP_NAME\|SWAP_STEP_NAME" apps/batch/src/main/kotlin/com/neki/batch/search/job/SearchIndexJob.kt` >= 2 |

## O-R. 릴리스

| id | 종류 | 판정 |
|---|---|---|
| O-R-1 | auto | `gh pr view feat/BACKEND-65 --json state,baseRefName --jq '.state + " " + .baseRefName'` = `OPEN main` (머지 전) |
| O-R-2 | auto | CI 통과 : `gh pr checks feat/BACKEND-65 --json name,state --jq '.[] \| select(.name=="Test") \| .state'` = `SUCCESS` |
| O-R-3 | manual | api 배포로 V32, V33 적용 뒤 `deploy-batch.yml` 로 이미지 push, Prefect `search-index` flow 수동 실행 -> k8s Job 종료 코드 0, `select count(*) from tb_photo_booth_search_read` > 0, `select platform, count(*) from tb_photo_booth_search_read group by 1` 에 platform 이 채워진 브랜드가 전부 보임 |
| O-R-4 | manual | 같은 `businessDate` 로 flow 를 한 번 더 실행 -> 두 테이블 건수 동일, `BATCH_JOB_INSTANCE` 에 인스턴스 하나 추가 |
| O-R-5 | manual | `update tb_brand set platform = ... where code = ...` 로 나머지 5개 브랜드 채운 뒤 재실행 -> 그 브랜드 카드가 생김 |
| O-R-6 | manual | 티켓 BACKEND-65 DONE (배포 뒤) |
| O-R-7 | manual | staging과 운영에서 swap 중 검색 API의 응답 시간과 DB 락 대기를 관찰하고, `BATCH_STEP_EXECUTION` 갱신과 커밋까지의 락 보유 시간이 검색 API의 합의된 허용 대기시간 이내인지 확인. 아래 절차로 결과와 허용 대기시간을 기록 |

### swap 락 보유 시간 확인 (O-R-7)

`lock_timeout`은 각 `RENAME`의 락 획득 대기시간을 제한하며, 획득한 락의 보유 시간이나 트랜잭션 전체 실행 시간을 제한하지 않습니다. `ACCESS EXCLUSIVE` 락은 `swap()` 반환 이후에도 TaskletStep의 `BATCH_STEP_EXECUTION` 갱신과 커밋이 끝날 때까지 유지됩니다.

- staging에서 검색 요청을 지속하며 잡을 실행하고, DB 모니터링의 `pg_locks`와 `pg_stat_activity`로 검색 카드 테이블의 `ACCESS EXCLUSIVE` 락과 조회 대기를 관찰합니다. 메타데이터 갱신 및 커밋 지연도 함께 확인합니다.
- 검색 API의 합의된 허용 대기시간과 관찰한 락 보유 시간, 응답 시간, 타임아웃 발생 여부를 기록합니다. 운영 실행에서도 같은 항목을 확인하며, 초과하면 검증 실패로 남기고 원인을 해소한 뒤 재검증합니다.
- swap부터 커밋까지의 경로에 외부 호출이나 추가 DB 작업을 넣지 않습니다. 트랜잭션 경계를 변경한다면 PostgreSQL에서 회전 도중 실패를 주입하여 카드·역 연결 테이블의 이름과 기존 `_read` 데이터가 모두 롤백되는지 재검증합니다. H2 테스트 통과만으로 PostgreSQL DDL 롤백을 검증했다고 판단하지 않습니다.

## 판정 규칙

- auto 하나라도 실패 = 미완료
- manual 은 PR 본문에 "배포 후 미검증" 으로 목록화, 실패 시 티켓 재오픈
- auto 전부 통과 + PR 생성 + CI 통과 = 에이전트 측 완료. manual 통과 = 기능 완료
