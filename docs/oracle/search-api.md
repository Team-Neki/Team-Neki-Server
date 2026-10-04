# 오라클 : 검색 부스 목록·브랜드 필터 실제 구현 (BACKEND-123)

이 문서는 BACKEND-123 작업의 완료 판정 기준과 staging 배포 후 검증 절차를 다룹니다. auto 는 명령이 판정하고, manual 은 배포 후 사람이 확인합니다. 명령은 저장소 루트에서 그대로 실행합니다.

## 무엇을 검증하는가

- 대상 API : `POST /api/search/photo-booths`, `POST /api/search/filter`. keyword 는 쿼리 파라미터가 아니라 body 로 받음(`keyword`, `filterGroup`, 목록만 `userLocation`). 필터 body 에 `userLocation` 처럼 모르는 필드를 넣으면 D-01
- keyword 해석 : QU(정규화 -> NER -> Intent)가 메모리 사전으로 지역·역·지점·브랜드를 먼저 뽑음. 자동완성 keyword(`서울특별시 강남구`, `강남역 2호선`, `모노맨션 강남역점`)도 사전에 있어 그 하나가 됨. 범위(지역·역·지점)를 하나도 못 찾았을 때만 DB 에서 자동완성 keyword 를 정확 일치로 찾음
- 조회 범위 : 지역은 `region_ids` 배열 포함(GIN), 역은 1km 연결 테이블(`_station`). 지역·역끼리는 합집합, 검색어의 브랜드와 요청의 브랜드 필터는 교집합
- 응답 id·favorite : 검색 색인 행의 원천 키 (platform, idx) 로 찾은 지도 부스(`TB_PHOTO_BOOTH_LOCATION`) 값. 지도에 없거나 `admin_hidden` 인 부스는 목록과 필터 모두에서 빠짐
- 정렬 : 사용자 위치가 있으면 거리 -> 지점명, 없으면 브랜드 -> 지점명 (검색 정책 12장)
- 결과 없음 : 지역·역을 찾지 못한 검색어도 D-04 가 아니라 빈 목록 (검색 정책 20장)
- NER 사전 : 앱이 뜰 때 메모리에 올리고 10분마다 다시 만듦 (`SearchDictionaryRefresher`)

```text
keyword -> QU(정규화 -> NER(메모리 사전) -> 범위가 없으면 자동완성 keyword 정확 일치(DB)) -> QueryIntent
        -> SearchCondition(범위: 지역·역·지점 / 조건: 브랜드) -> 검색 색인 조회
        -> 지도 부스(map, id·즐겨찾기·숨김) -> SearchedBooths(짝짓기, 정렬, 브랜드 집계) -> 응답
```

## NER 해석 규칙

NER 은 정규화 검색어(소문자, 공백·`-`·`_` 제거) 안에서 사전에 있는 이름을 찾아 엔티티로 만드는 QU 의 한 단계입니다. 아래 규칙이 O-A-12 의 단위 테스트와 O-R-14 의 staging 로그가 확인하는 기준입니다.

- 사전 원천 : 지역은 시군구 이하 법정동(`tb_legal_dong`), 역은 `tb_subway_station`(`SubwayStationRepository`), 지점·브랜드는 검색 색인(`_read`)
- 지역 항목 : 법정동 전체 경로(`서울특별시 강남구`, `경상남도 진주시 강남동`). 서울 자치구는 이름(`강남구`)과 줄임말(`강남`)도 넣고, 줄임말이 한 글자면(`중구` -> `중`) 넣지 않음
- 역 항목 : `역명역`(노선마다 한 항목이라 `강남역` 은 2호선·신분당선 모두)과 `역명역 노선명`(그 노선 하나)
- 지점 항목 : 색인의 `브랜드명 지점명`. 찾으면 엔티티 종류는 BRANCH, 대상은 그 지점(Booth)
- 브랜드 항목 : 색인에 있는 브랜드 이름 (동의어·오타는 다루지 않음)
- 겹침 해소 : longest match. 긴 범위가 먼저, 길이가 같으면 앞선 범위가 이기고 겹치는 범위는 버림. 그래서 자동완성 keyword 는 그 안의 짧은 이름(`강남`, `강남역`, 브랜드)이 아니라 keyword 전체 하나가 됨
- 지점명 제외 : 겹침을 해소한 뒤 지점 접미사(`점`, `본점`, `N호점`)가 바로 뒤에 붙은 지역·역은 지점명의 일부로 보고 버림
- 지점 조각 : 엔티티 사이 조각이 지점 접미사로 끝나면 BRANCH 엔티티가 되지만 대상이 없어 범위를 좁히지 않음
- DB 정확 일치 : 범위(지역·역·지점)가 하나도 없을 때만 봄. 사전이 아직 모르는 keyword(갱신 주기 사이에 색인에 들어온 지점, 사전 적재 실패)를 위한 것
- 사전이 비어 있으면 NER 은 아무것도 찾지 않고, 자동완성 keyword 는 DB 정확 일치로 동작함

서버 로그 `[SEARCH] api=photo-booths` 의 entities 는 `종류:정규화 범위` 로 찍힙니다. 한 범위가 대상 여럿을 가리키면 그 수만큼 반복됩니다.

| 검색어 | 기대 entities | 규칙 |
|---|---|---|
| `서울특별시 강남구` | `[REGION:서울특별시강남구]` | 전체 경로 |
| `경상남도 진주시 강남동` | `[REGION:경상남도진주시강남동]` | 전체 경로가 `강남`(강남구)보다 길어 이김 |
| `강남` | `[REGION:강남]` | 서울 자치구 줄임말 |
| `포토이즘 강남역` | `[BRAND:포토이즘, STATION:강남역, STATION:강남역]` | `강남역` 은 노선마다 |
| `강남역 2호선` | `[STATION:강남역2호선]` | 그 노선 하나 |
| `모노맨션 강남역점` (색인에 있는 지점) | `[BRANCH:모노맨션강남역점]` | 지점 하나. 그 안의 `강남역` 은 겹쳐서 버림 |
| `포토이즘 강남역점` (사전에 없는 지점) | `[BRAND:포토이즘, BRANCH:강남역점]` | 지점명 속 역은 버림. 범위가 없어 DB 를 보고, 없으면 빈 목록 |
| `부산진구 포토이즘` | `[BRAND:포토이즘]` | 서울 밖 자치구 이름은 사전에 없음. 빈 목록 |

## 공통 (O-0)

| id | 종류 | 판정 | 명령/절차 |
|---|---|---|---|
| O-0-1 | auto | 포맷 검사 통과 | `./gradlew spotlessCheck -q` 종료코드 0 |
| O-0-2 | auto | 테스트 전부 통과, 모듈별 수 >= baseline (domain 71, apps/batch 18, apps/api 608) | `./gradlew test -q` 종료코드 0 뒤 `for m in domain apps/batch apps/api; do echo "$m $(grep -ho 'tests="[0-9]*"' $m/build/test-results/test/*.xml \| grep -o '[0-9]*' \| paste -sd+ - \| bc)"; done` |
| O-0-3 | auto | 코드 산출물에 이모지 없음 | `git diff origin/main...HEAD -- '*.kt' '*.kts' '*.sql' '*.yaml' '*.yml' '*.http' \| perl -CSD -ne 'print if /^\+.*[\x{1F300}-\x{1FAFF}\x{2600}-\x{27BF}]/' \| wc -l` = 0 |
| O-0-4 | auto | 도메인 격리 규칙 통과 (search 는 map 을 client 로만 부름) | `./gradlew :apps:api:test --tests 'com.neki.api.rule.ArchitectureRulesTest' -q` 종료코드 0 |

## O-A. 구현 (auto)

| id | 판정 | 명령 |
|---|---|---|
| O-A-1 | mock 제거 | `test ! -e apps/api/src/main/kotlin/com/neki/api/search/application/SearchMockData.kt` 종료코드 0 |
| O-A-2 | 부스 목록·필터·자동완성 E2E 통과 | `./gradlew :apps:api:test --tests 'com.neki.api.e2e.search.*' -q` 종료코드 0 |
| O-A-3 | 목록과 필터는 같은 부스 집합을 본다 (칩 개수 합계 = 목록 건수). 두 유스케이스가 흐름을 따로 갖는 대신 이 테스트가 어긋남을 잡음 | `./gradlew :apps:api:test --tests 'com.neki.api.e2e.search.GetSearchFilterE2ETest' -q` 종료코드 0 |
| O-A-4 | QU 호출은 유스케이스가 하고, 조회 서비스는 QU 를 모름 | `grep -c "queryUnderstandingService.understand" apps/api/src/main/kotlin/com/neki/api/search/application/SearchPhotoBoothsUseCase.kt apps/api/src/main/kotlin/com/neki/api/search/application/GetSearchFilterUseCase.kt` 두 파일 모두 1, `grep -c "QueryUnderstandingService" domain/src/main/kotlin/com/neki/domain/search/service/PhotoBoothSearchService.kt` = 0 |
| O-A-5 | NER·지점명 규칙은 domain 모듈 밖에서 못 쓰고, QueryIntent 는 팩토리로만 만듦 | `grep -c "internal object" domain/src/main/kotlin/com/neki/domain/search/service/qu/Ner.kt domain/src/main/kotlin/com/neki/domain/search/service/qu/BranchNamePolicy.kt` 두 파일 모두 1, `grep -c "class QueryIntent private constructor" domain/src/main/kotlin/com/neki/domain/search/models/qu/QueryIntent.kt` = 1 |
| O-A-6 | NER 사전은 요청마다 조회하지 않고 메모리에서 꺼냄. 기동 시와 10분마다 갱신 | `grep -c "entityDictionaryCache.get()" domain/src/main/kotlin/com/neki/domain/search/service/qu/QueryUnderstandingService.kt` = 1, `./gradlew :apps:api:test --tests 'com.neki.api.search.infra.scheduler.SearchDictionaryRefresherTest' -q` 종료코드 0 |
| O-A-7 | 짝짓기·정렬·브랜드 집계는 도메인 컬렉션(SearchedBooths)이 하고 Assembler 는 옮겨 담기만 함 | `grep -c "sortedWith\|groupBy" apps/api/src/main/kotlin/com/neki/api/search/application/dto/SearchAssembler.kt` = 0, `./gradlew :domain:test --tests 'com.neki.domain.search.SearchedBoothsTest' -q` 종료코드 0 |
| O-A-8 | 역 keyword 형식(`역명역 노선명`)은 SubwayStation 한 곳 | `grep -rn '"역"' domain/src/main/kotlin/com/neki/domain/search/models domain/src/main/kotlin/com/neki/domain/search/service/qu \| grep -vc SubwayStation.kt` = 0 |
| O-A-9 | batch 컨텍스트가 뜬다 (batch 가 스캔하는 search 도메인 서비스가 apps/api 전용 빈에 기대지 않음) | `grep -rc "MapClient" domain/src/main/kotlin/com/neki/domain/search/service \| grep -v ":0" \| wc -l` = 0, `./gradlew :apps:batch:test -q` 종료코드 0 |
| O-A-10 | 지역 조회는 GIN 을 타는 배열 포함 연산 | `grep -c "array_contains" domain/src/main/kotlin/com/neki/domain/search/infra/persist/jpa/PhotoBoothSearchQueryRepository.kt` >= 1 |
| O-A-11 | 스키마 변경 없음 (V34 컬럼을 엔티티에 매핑만 함) | `git diff --name-only origin/main...HEAD -- modules/postgres/src/main/resources/db/migration \| wc -l` = 0 |
| O-A-12 | "NER 해석 규칙" 이 지켜짐 : 사전 항목, longest match, 지점명 제외, 지점 조각, 범위를 찾으면 DB 를 보지 않음(stub 없는 저장소로 확인) | `./gradlew :domain:test --tests 'com.neki.domain.search.service.qu.*' --tests 'com.neki.domain.search.models.qu.*' --tests 'com.neki.domain.search.BranchNamePolicyTest' --tests 'com.neki.domain.search.SearchNormalizerTest' -q` 종료코드 0 |
| O-A-13 | 사전 원천 : 지역은 법정동 전체, 역은 SubwayStationRepository, 지점·브랜드는 색인 | `grep -c "legalDongRepository.findAllBelowSido()\|subwayStationRepository.findAll()\|photoBoothSearchRepository.findAllCurrent()\|photoBoothSearchRepository.findIndexedBrandNames()" domain/src/main/kotlin/com/neki/domain/search/service/qu/QueryUnderstandingService.kt` = 4, `grep -c "findAllStations" domain/src/main/kotlin/com/neki/domain/search/service/qu/QueryUnderstandingService.kt` = 0 |
| O-A-14 | 목록·필터 API 는 keyword 를 body 로 받고 쿼리 파라미터로는 받지 않음 | `grep -c 'val keyword: String?' apps/api/src/main/kotlin/com/neki/api/search/api/dto/SearchRequest.kt` = 2, `grep -A4 '@PostMapping' apps/api/src/main/kotlin/com/neki/api/search/api/controller/SearchController.kt \| grep -c RequestParam` = 0, O-A-2 종료코드 0 |

## O-R. 배포 후 (manual)

요청 id(`[PB-1]` 등)는 `http/search.http` 의 요청 이름이고, SQL id(`S-1` 등)는 아래 "staging 검증 절차" 의 쿼리입니다.

| id | 판정 | 확인 방법 |
|---|---|---|
| O-R-1 | 선행 데이터가 있다 : 검색 색인, 지도 부스(COLLECTED), 둘이 원천 키로 짝지어지는 부스, 서울 자치구 25개, 시군구 이하 법정동, 지하철역 | S-1 ~ S-4 |
| O-R-2 | NER 사전이 올라간다 : `[SEARCH] dictionary refreshed entries=` 가 기동 직후 S-8 기대값과 같게 찍히고 `dictionary refresh failed` 가 없다 | 4단계 로그, S-8 |
| O-R-3 | 자동완성 keyword 로 부스 목록이 나온다 : 지역 keyword 로 items > 0, 거리순(같으면 지점명순), 위치가 없으면 브랜드·지점명순. 건수가 S-5 와 같다 | [C-1], [PB-1], [PB-2], S-5 |
| O-R-4 | 역 keyword 로 1km 안 부스가 나온다 : 건수가 S-6 과 같다 | [C-2], [PB-3], S-6 |
| O-R-5 | 목록과 필터가 같은 부스를 센다 : 필터 count 합계 = 목록 건수, 칩 순서 = 사용자 브랜드 순서 | [B-1], [F-1], [F-2] |
| O-R-6 | 자유 검색어를 이해한다 : `강남구 포토이즘` 과 `포토이즘 강남구` 가 같은 결과(강남구의 포토이즘만), `인생네컷 강남역` 은 인생네컷만, 검색어 브랜드와 겹치지 않는 브랜드 필터는 빈 목록. 서버 로그 `[SEARCH] api=photo-booths` 의 entities 가 기대와 같다 | [PB-4] ~ [PB-7], 5단계 로그 |
| O-R-7 | 결과 없음은 에러가 아니다 : 서울 밖 자치구, 브랜드만, 없는 지역은 200 빈 목록. 공백 keyword 는 400 D-01, 토큰 없으면 403 | [PB-8a] ~ [PB-8c], [PB-9], [PB-10] |
| O-R-8 | 즐겨찾기가 반영된다 : 목록의 부스를 즐겨찾기하면 다시 조회했을 때 favorite = true. 확인 뒤 되돌린다 | [FAV-1] ~ [FAV-3] |
| O-R-9 | 지역 조회가 GIN 인덱스를 쓸 수 있다 | S-7 |
| O-R-10 | 사전 갱신이 메모리를 쌓지 않는다 : 30분 이상(갱신 3회 이상) 지나도 `heapUsedMb` 가 계속 오르기만 하지 않는다 | 7단계 로그 |
| O-R-11 | 티켓 BACKEND-123 DONE (배포와 검증 뒤) | Sprint |
| O-R-12 | 부스 자동완성 keyword 로 그 지점 하나가 나온다 : 자동완성 응답 keyword 를 그대로 넘기면 그 이름의 지점만 나온다(보통 1건). 그 지점의 브랜드로 거르면 그대로, 다른 브랜드로 거르면 빈 목록. 필터는 그 브랜드 1개 | [C-3], [PB-11] ~ [PB-13], [F-3] |
| O-R-13 | 브랜드가 섞인 검색어(`강남구 포토이즘`, `강남역 2호선 포토이즘`, `포토이즘 강남역`, `서울특별시 강남구 포토이즘`)에서 지역·역·부스 자동완성의 첫 후보 keyword 와 자동완성 응답의 filterGroup 을 그대로 목록·필터 body 에 넣으면 그 브랜드만 나온다(자동완성 filterGroup 은 BACKEND-207). 필터 칩은 그 브랜드 하나이고 count 는 목록 건수와 같다. 검색어를 그대로 넘긴 기준선(BK-0)과 같은 브랜드여야 한다 | `http/search-brand-keyword.http` [BK-0] ~ [BK-9] |
| O-R-14 | NER 이 자동완성 keyword 를 그 하나로 해석한다 : 로그 entities 가 [PB-1] `[REGION:서울특별시강남구]`, [PB-3] `[STATION:강남역2호선]` 하나(노선 둘이 아님), [PB-11] `[BRANCH:...]` 하나, [PB-8d] `[REGION:경상남도진주시강남동]` 이고 [PB-8d] 응답에 서울 부스가 없다 | [PB-1], [PB-3], [PB-8d], [PB-11], 5단계 로그 |

## staging 검증 절차

staging 은 `https://dev-yapp.suitestudy.com:4641` 이고 배포는 `deploy-api-staging.yml` 을 수동 실행합니다. 각 단계의 결과는 PR 본문 "배포 후 검증" 에 항목 id 와 함께 남깁니다.

### 1. 배포 전 : #330 최신과 맞춘다

이 브랜치는 `feat/BACKEND-152`(#330) 위에 쌓여 있습니다. #330 에 새 커밋이 생기면 그 기능(부스 자동완성, `distanceKm` 등)이 이 브랜치에 없으므로, 배포 전에 아래가 종료코드 0 인지 봅니다. 아니면 `origin/feat/BACKEND-152` 를 merge 하고 `./gradlew test` 를 다시 통과시킵니다.

```bash
git fetch origin && git merge-base --is-ancestor origin/feat/BACKEND-152 HEAD
```

- 충돌이 나면 : `SearchMapClient.kt` 는 한 클래스가 `PhotoBoothClient` 와 `MapClient` 를 함께 구현하도록 합치고, `SearchController.kt`·`SearchConverter.kt` 는 자동완성 쪽은 #330, 부스 목록·필터 쪽은 이 브랜치를 따름

### 2. 선행 데이터 확인 (O-R-1)

staging DB(`dev_yapp`)에서 실행합니다. 노드(4단계의 SSH 접속)에서 `sudo -u postgres psql -d dev_yapp` 로 들어가 조회만 합니다. 하나라도 0 이면 두 API 는 빈 목록만 돌려주므로 원인(색인 잡, stores-sync, Workflow 적재)을 먼저 해결합니다.

```sql
-- S-1 검색 색인 (searchIndexJob) : > 0
select count(*) from tb_photo_booth_search_read;

-- S-2 지도 부스 중 수집 지점 (stores-sync, BACKEND-153) : > 0
select count(*) from TB_PHOTO_BOOTH_LOCATION where source_type = 'COLLECTED';

-- S-3 색인 행과 지도 부스가 원천 키로 짝지어지는 부스 (실제로 목록에 나올 수 있는 부스) : > 0
select count(*)
from tb_photo_booth_search_read s
join TB_PHOTO_BOOTH_LOCATION l
  on l.source_platform = s.platform and l.source_idx = s.idx and l.admin_hidden = false;

-- S-4 NER 사전 원천 : 서울 자치구 = 25, 시군구 이하 법정동 > 0, 지하철역 > 0
select count(*) from tb_legal_dong where level = 2 and code like '11%';
select count(*) from tb_legal_dong where level > 1;
select count(*) from tb_subway_station;
```

### 3. 배포

staging 은 여러 PR 이 함께 쓰므로 이 브랜치를 바로 배포하지 않고 검증용 `stg` 브랜치에 merge 해서 배포합니다. 바로 배포하면 `stg` 에만 있는 다른 PR 의 기능이 staging 에서 사라집니다.

```bash
git push origin feat/BACKEND-123
# stg 워크트리에서
git pull --ff-only origin stg && git merge --no-ff feat/BACKEND-123 -m "merge: feat/BACKEND-123 을 staging 검증용 stg 에 병합"
./gradlew test && git push origin stg
gh workflow run deploy-api-staging.yml --ref stg
gh run watch "$(gh run list --workflow deploy-api-staging.yml --limit 1 --json databaseId --jq '.[0].databaseId')"
```

워크플로가 GitOps 이미지 태그를 갱신하면 ArgoCD 반영까지 약 2분이 걸립니다. `curl -s -o /dev/null -w "%{http_code}" https://dev-yapp.suitestudy.com:4641/actuator/health` 가 200 이 되면 다음 단계로 넘어갑니다.

### 4. 사전 적재 로그 확인 (O-R-2)

staging 로그는 Loki 에 수집되지 않으므로(Loki·Prometheus 는 prod 만 수집) 노드에서 `kubectl` 로 봅니다. 노드 접속은 Sprint 위키 "prefect tunneling" 의 계정을 씁니다.

```bash
ssh -p 2024 yapp@suitestudy.com
kubectl logs -n staging deploy/yapp-app-staging | grep -F '[SEARCH] dictionary'
```

```text
[SEARCH] dictionary refreshed entries=24871 heapUsedMb=310 heapCommittedMb=512 heapMaxMb=1024
```

- entries : 아래 S-8 과 같아야 함. 0 이면 S-4 를 다시 확인
- `dictionary refresh failed` 가 있으면 스택트레이스로 원인 확인. 이때 자동완성 keyword 는 동작하지만 자유 검색어는 빈 목록이 됨
- 위 예시의 숫자는 형식을 보이기 위한 값이며 실제 값은 데이터에 따라 다름

```sql
-- S-8 NER 사전 항목 수 기대값. 색인이 바뀌는 05:30 KST 직후 10분 안에는 어긋날 수 있음
select (select count(*) from tb_legal_dong where level > 1)                -- 지역 전체 경로
     + 49                                                                   -- 서울 자치구 이름 25 + 줄임말 24 (중구는 줄임말 없음)
     + 2 * (select count(*) from tb_subway_station)                         -- `역명역`, `역명역 노선명`
     + (select count(*) from tb_photo_booth_search_read)                    -- 지점 `브랜드명 지점명`
     + (select count(distinct brand_id) from tb_photo_booth_search_read)    -- 브랜드
  as expected_entries;
```

### 5. 요청 실행 (O-R-3 ~ O-R-8, O-R-12, O-R-14)

IntelliJ HTTP Client 로 `http/search.http` 를 `staging` 환경에서 위에서부터 순서대로 실행합니다. 앞 요청이 저장한 값(브랜드 id, 건수, 부스 id)을 뒤 요청이 쓰므로 하나씩 건너뛰지 않습니다.

- 토큰 : `http/http-client.private.env.json` 에 `{ "staging": { "accessToken": "<JWT>" } }` 로 둠 (`.gitignore` 대상)
- 각 요청의 `client.test` 가 판정함. 실패한 요청은 응답 body 와 함께 PR 에 남김
- [PB-1], [PB-3] 은 응답 id 를 로그로 남기므로 S-5, S-6 과 건수를 맞춰 봄
- [PB-4] ~ [PB-7] 을 실행한 시각의 서버 로그에서 `[SEARCH] api=photo-booths keyword="강남구 포토이즘" entities=[REGION:강남구, BRAND:포토이즘]` 처럼 엔티티가 찍혔는지 확인
- [FAV-1] ~ [FAV-3] 은 토큰 사용자의 즐겨찾기를 잠깐 바꿨다가 되돌림. [FAV-3] 까지 반드시 실행
- [C-3] 은 부스 자동완성 첫 후보를 저장하고 [PB-11] ~ [PB-13], [F-3] 이 그 keyword 를 그대로 씀. 서버 로그 entities 가 `[BRANCH:...]` 로 찍히는지 확인
- O-R-14 : [PB-1], [PB-3], [PB-8d], [PB-11] 의 서버 로그 entities 를 "NER 해석 규칙" 의 기대 entities 와 맞춰 봄. 자동완성 keyword 가 엔티티 여러 개로 쪼개져 찍히면 사전에 그 keyword 가 없다는 뜻이므로 S-8 과 entries 를 다시 비교

```sql
-- S-5 [PB-1] 건수와 같아야 함 (강남구, 지도에 있고 숨기지 않은 부스)
select count(*)
from tb_photo_booth_search_read s
join TB_PHOTO_BOOTH_LOCATION l
  on l.source_platform = s.platform and l.source_idx = s.idx and l.admin_hidden = false
where s.region_ids @> array['1168000000']::varchar[];

-- S-6 [PB-3] 건수와 같아야 함 (강남역 2호선 1km 안)
select count(*)
from tb_photo_booth_search_read s
join tb_photo_booth_search_read_station st on st.search_id = s.id
join TB_PHOTO_BOOTH_LOCATION l
  on l.source_platform = s.platform and l.source_idx = s.idx and l.admin_hidden = false
where st.station_name = '강남' and st.line_name = '2호선';
```

### 6. 실행 계획 확인 (O-R-9)

애플리케이션이 보내는 지역 조건(`region_ids @> cast(array[?] as varchar(10) array)`)과 같은 모양으로 실행 계획을 봅니다.

```sql
-- S-7
explain select * from tb_photo_booth_search_read
where region_ids @> cast(array['1168000000'] as varchar(10)[]);
```

- 기대 : `Bitmap Index Scan on ix_pbs_a_region_ids`(또는 `_b_`, 슬롯은 swap 마다 바뀜)
- `Seq Scan` 이 나오면 같은 세션에서 `set enable_seqscan = off;` 뒤 다시 실행. 인덱스 스캔으로 바뀌면 통과 (행이 수천 건이라 planner 가 seq scan 을 고르는 것은 정상)

### 7. 메모리 추세 확인 (O-R-10)

배포 후 30분 이상 지나 4단계와 같은 검색으로 `dictionary refreshed` 로그를 3건 이상 모읍니다. 사전에 법정동 전체 경로(2만여 건)와 지점이 들어가 갱신마다 이 행들을 다시 읽으므로, 이전 배포보다 힙이 한 단계 높은 것은 정상입니다. `heapUsedMb` 는 JVM 힙 전체 스냅샷이라 사전 크기 자체가 아니며 GC 시점에 따라 오르내립니다. 갱신을 거듭해도 계속 오르기만 하면 이전 사전이 회수되지 않는 것을 의심할 수 있습니다. staging 은 Prometheus 가 수집하지 않으므로 이 로그의 `heapUsedMb` 와 `kubectl get pod` 의 RESTARTS(OOM 재시작 여부)로 판단합니다. staging 파드의 최대 heap 은 247MB 입니다.

### 8. 기록

- PR 본문 "배포 후 검증" 에 O-R-1 ~ O-R-10 의 결과를 id 별로 남김
- 실패 항목이 있으면 티켓 BACKEND-123 을 IN_PROGRESS 로 두고 원인을 댓글로 남김
- staging 을 #330 최신으로 되돌려야 하면 `gh workflow run deploy-api-staging.yml --ref feat/BACKEND-152` 로 다시 배포

## 판정 규칙

- auto 하나라도 실패 = 미완료
- manual 은 PR 본문에 "배포 후 미검증" 으로 목록화, 실패 시 티켓 재오픈
- auto 전부 통과 + PR 생성 + CI 통과 = 에이전트 측 완료. manual 통과 = 기능 완료
