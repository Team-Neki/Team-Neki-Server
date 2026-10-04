# 오라클 : 부스 자동완성 검색 색인 전환과 자동완성 filterGroup (BACKEND-207)

이 문서는 BACKEND-207 작업의 완료 판정 기준과 staging 배포 후 검증 절차를 다룹니다. auto 는 명령이 판정하고, manual 은 배포 후 사람이 확인합니다. 명령은 저장소 루트에서 그대로 실행합니다. 베이스는 `feat/BACKEND-123` 이며 그 오라클(`docs/oracle/search-api.md`)도 계속 통과해야 합니다.

## 무엇을 검증하는가

- `GET /api/search/completion/photo-booths` : `TB_PHOTO_BOOTH_LOCATION` 대신 부스 목록·필터 API 와 같은 검색 색인(`tb_photo_booth_search_read`)을 조회. 매칭·정렬 규칙은 그대로이고 이름 비교만 정규화 컬럼 기준(공백·`-`·`_`·대소문자 무시)
- `GET /api/search/completion/regions`, `/stations`, `/photo-booths` : 응답 `data.filterGroup` 이 부스 목록·필터 요청의 `SearchRequest.FilterGroup` 과 같은 타입이라 body 의 filterGroup 으로 그대로 보낼 수 있음
  - 검색어에 브랜드 이름 전체가 있으면(NER 사전 기준) `brandFilter.brands = [{brandId}]`, 없으면 `[]`. `sortFilter.type` 은 `DEFAULT`
  - 정하지 않은 `sortFilter.order` 는 필드째 빠짐
- 지역·역 자동완성이 검색어에서 브랜드 낱말을 뺄 때 쓰는 브랜드 이름을 요청마다 `tb_brand` 에서 조회하지 않고 NER 사전(검색 색인의 브랜드, 10분 갱신)에서 꺼냄. 판정 규칙(`CompletionKeyword.withoutBrandWords`)은 그대로
- 알려진 차이 : 색인은 지도 숨김(`admin_hidden`)·지도 미동기화 지점도 갖고 있어 자동완성에 나올 수 있음. 그 keyword 의 부스 목록은 빈 배열 (이전 구현도 숨김 지점을 거르지 않았음)

## 공통 (O-0)

| id | 종류 | 판정 | 명령/절차 |
|---|---|---|---|
| O-0-1 | auto | 포맷 검사 통과 | `./gradlew spotlessCheck -q` 종료코드 0 |
| O-0-2 | auto | 테스트 전부 통과, 모듈별 수 >= baseline (domain 89, apps/batch 18, apps/api 653) | `./gradlew test -q` 종료코드 0 뒤 `for m in domain apps/batch apps/api; do echo "$m $(grep -ho 'tests="[0-9]*"' $m/build/test-results/test/*.xml \| grep -o '[0-9]*' \| paste -sd+ - \| bc)"; done` |
| O-0-3 | auto | 도메인 격리 규칙 통과 | `./gradlew :apps:api:test --tests 'com.neki.api.rule.ArchitectureRulesTest' -q` 종료코드 0 |

## O-A. 구현 (auto)

| id | 판정 | 명령 |
|---|---|---|
| O-A-1 | 부스 자동완성이 검색 색인을 조회하고, map 쪽 keyword 검색 경로는 남아 있지 않음 | `grep -c "photoBoothSearchService.searchByKeyword" apps/api/src/main/kotlin/com/neki/api/search/application/SearchPhotoBoothsByKeywordUseCase.kt` = 1, `grep -rn "SearchPhotoBoothLocationsUseCase\|MapQuery.SearchPhotoBooths\|PhotoBoothSummary" apps/api/src/main domain/src/main \| wc -l` = 0 |
| O-A-2 | 자동완성·부스 목록 E2E 통과 (keyword 매칭·정렬, filterGroup, 고른 keyword + filterGroup 왕복) | `./gradlew :apps:api:test --tests 'com.neki.api.e2e.search.*' -q` 종료코드 0 |
| O-A-4 | 브랜드 낱말 빼기가 DB 를 조회하지 않음 | `grep -rn "photoBoothClient\|GetBrandNamesUseCase\|PhotoBoothClient" apps/api/src/main domain/src/main \| wc -l` = 0, `grep -c "queryUnderstandingService.brandNames()" apps/api/src/main/kotlin/com/neki/api/search/application/SearchRegionsUseCase.kt apps/api/src/main/kotlin/com/neki/api/search/application/SearchStationsUseCase.kt` 두 파일 모두 1 |
| O-A-3 | 스키마 변경 없음 | `git diff --name-only origin/feat/BACKEND-123...HEAD -- modules/postgres/src/main/resources/db/migration \| wc -l` = 0 |

## O-R. 배포 후 (manual)

| id | 판정 | 근거 |
|---|---|---|
| O-R-1 | 역 자동완성 : 브랜드 없는 검색어는 기본 filterGroup, `강남 포토이즘` 은 포토이즘이 걸린 filterGroup | `http/search-completion-filter-group.http` [FG-1], [FG-2] |
| O-R-2 | 역 자동완성 첫 후보와 filterGroup 을 그대로 부스 목록에 보내면 포토이즘만 나온다 | [FG-3] |
| O-R-3 | 부스 자동완성이 색인에서 후보를 내리고, 첫 후보와 filterGroup 을 그대로 보내면 그 지점이 나온다 | [FG-4], [FG-5] |
| O-R-4 | 지역 자동완성 : `강남 포토이즘` 은 포토이즘이 걸린 filterGroup | [FG-6] |
| O-R-5 | 티켓 BACKEND-207 DONE (배포와 검증 뒤) | Sprint |

## 판정 규칙

auto 가 전부 통과하면 PR 을 올리고, staging 배포 후 manual 이 전부 통과하면 티켓을 닫습니다. 하나라도 실패하면 원인을 PR 에 남기고 고친 뒤 처음부터 다시 판정합니다.
