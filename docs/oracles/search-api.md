# 통합 검색 API 배포 후 검증 오라클

이 문서는 통합 검색 mock API(`POST /api/search/photo-booths`, `POST /api/search/filter`)를 배포한 뒤 응답이 기대값과 맞는지 확인하는 오라클(test oracle)에 대해 다룹니다. 오라클은 "이 요청을 보내면 이 응답이 와야 한다"를 미리 못 박아 둔 표이며, 같은 번호의 요청문이 `http/search.http` 에 들어 있습니다.

## 실행 방법

`http/search.http` 는 IntelliJ HTTP Client 형식입니다. 환경은 `http/http-client.env.json` 에서 고릅니다.

| 환경    | host                                  |
|---------|---------------------------------------|
| local   | http://localhost:8080                 |
| staging | https://dev-yapp.suitestudy.com:4641  |
| prod    | https://yapp.suitestudy.com:4641      |

토큰은 다음 순서로 얻습니다.

1. 브라우저에서 카카오 authorize URL 을 열어 `id_token` 을 얻음 (URL 은 `AuthController.oauthLogin` 의 Swagger 설명 참고. `redirect_uri` 가 환경마다 다름)
2. `http/http-client.private.env.json` 에 `kakaoIdToken` 을 넣음 (git 에 올라가지 않음)
3. `search.http` 의 `[AUTH]` 요청을 실행하면 응답의 `accessToken` 이 전역 변수로 저장됨

```json
{
  "staging": { "kakaoIdToken": "<id_token>" }
}
```

각 요청에는 응답 핸들러가 붙어 있어 실행하면 status, resultCode, 핵심 값이 자동으로 검증됩니다. 아래 오라클 표는 그 핸들러가 보는 값과 같습니다. 같은 케이스가 `apps/api/src/test/kotlin/com/neki/api/e2e/search/` 의 E2E 테스트에도 있으므로, 표를 고치면 테스트와 핸들러도 함께 고쳐야 합니다.

## mock 응답

mock 은 keyword, filterGroup, userLocation 을 전부 무시하고 항상 같은 값을 내려줍니다. 요청 검증(keyword 필수, filterGroup 필수, 토큰)만 실제와 같습니다. 따라서 어느 환경에서 어떤 유효한 요청을 보내든 아래 표와 같아야 하며, 값이 다르면 배포된 코드가 다른 것입니다.

`POST /api/search/photo-booths` 의 `data.items` 는 다음 6개이고 순서도 고정입니다. distance 는 강남역(37.4979, 127.0276) 기준 haversine 거리(m)입니다.

| 순서 | id   | 브랜드 (code)          | 지점     | 주소                        | 위도, 경도               | distance | favorite |
|------|------|------------------------|----------|-----------------------------|--------------------------|----------|----------|
| 0    | 2591 | 포토이즘 (PHOTOISM)     | 강남역점 | 서울 강남구 강남대로 372     | 37.4967118, 127.0289042 | 175      | false    |
| 1    | 3115 | 인생네컷 (LIFEFOURCUTS) | 강남2호점 | 서울 서초구 강남대로 419     | 37.4995520, 127.0256833 | 250      | false    |
| 2    | 3102 | 인생네컷 (LIFEFOURCUTS) | 강남역점 | 서울 강남구 강남대로96길 12  | 37.5002916, 127.0288671 | 288      | false    |
| 3    | 2573 | 포토이즘 (PHOTOISM)     | 강남2호점 | 서울 서초구 서초대로77길 31  | 37.5006179, 127.0253775 | 360      | false    |
| 4    | 2604 | 포토이즘 (PHOTOISM)     | 역삼점   | 서울 강남구 테헤란로 123     | 37.4998310, 127.0316420 | 416      | false    |
| 5    | 2560 | 포토이즘 (PHOTOISM)     | 강남1호점 | 서울 강남구 강남대로102길 16 | 37.5021077, 127.0271830 | 469      | true     |

`POST /api/search/filter` 의 `data.brandFilter` 는 다음 2개이고 브랜드 ID 순입니다.

| 순서 | id | name     | code         | count |
|------|----|----------|--------------|-------|
| 0    | 1  | 포토이즘 | PHOTOISM     | 4     |
| 1    | 2  | 인생네컷 | LIFEFOURCUTS | 2     |

## 요청 형식

두 API 모두 keyword 는 쿼리 파라미터, 나머지는 JSON body 입니다.

```http
POST /api/search/photo-booths?keyword=강남
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "filterGroup": {
    "brandFilter": { "brands": [{ "brandId": 2 }] },
    "sortFilter": { "type": "DEFAULT", "order": "NONE" }
  },
  "userLocation": { "latitude": 37.4979, "longitude": 127.0276 }
}
```

- `keyword` : 필수. 없거나 공백만 있으면 D-01
- `filterGroup` : 필수. 필터를 안 걸려면 `{}`
- `userLocation` : photo-booths 만 받음. filter 는 필드 자체가 없음

## 오라클 : POST /api/search/photo-booths

| #    | 요청                                                                    | HTTP | resultCode | 기대 응답                                    |
|------|-------------------------------------------------------------------------|------|------------|----------------------------------------------|
| PB-1 | keyword=강남, filterGroup {}, userLocation 강남역                         | 200  | D-0        | 위 items 6개 표와 동일                         |
| PB-2 | keyword=송정, brands [{brandId: 2}], sortFilter DESC, userLocation 없음   | 200  | D-0        | PB-1 과 body 가 완전히 동일 (입력 무시 확인)   |
| PB-3 | keyword 없음                                                            | 400  | D-01       | -                                            |
| PB-4 | keyword 가 공백 한 칸                                                    | 400  | D-01       | -                                            |
| PB-5 | body `{}` (filterGroup 없음)                                             | 400  | D-01       | -                                            |
| PB-6 | Authorization 없음                                                      | 403  | D-996      | -                                            |

## 오라클 : POST /api/search/filter

| #    | 요청                                  | HTTP | resultCode | 기대 응답                                    |
|------|---------------------------------------|------|------------|----------------------------------------------|
| FT-1 | keyword=강남, filterGroup {}           | 200  | D-0        | 위 brandFilter 2개 표와 동일                   |
| FT-2 | keyword=송정, brands [{brandId: 2}]    | 200  | D-0        | FT-1 과 body 가 완전히 동일 (입력 무시 확인)   |
| FT-3 | keyword 없음                          | 400  | D-01       | -                                            |
| FT-4 | keyword 가 공백 한 칸                  | 400  | D-01       | -                                            |
| FT-5 | Authorization 없음                    | 403  | D-996      | -                                            |

## 값이 다를 때

- 200 인데 items 나 count 가 표와 다름 : 배포된 커밋의 `SearchMockData` 가 이 문서와 다른 것. 실제 구현으로 넘어갔다면 이 문서는 폐기 대상
- PB-2 가 PB-1 과 다름 (개수가 줄거나 distance 가 null) : 입력을 반영하는 이전 mock 또는 실제 구현이 배포된 것
- PB-4, FT-4 가 400 이 아니라 200 : `@RequestParam @NotBlank` 검증이 안 걸린 것. validation starter 와 `HandlerMethodValidationException` 핸들러 확인
- 403 대신 401 : 토큰이 있는데 만료된 것. `[AUTH]` 요청을 다시 실행

## 정리

**정리하면, 검색 mock API 는 환경과 입력에 관계없이 위 표와 같은 값을 내려줘야 하며, `http/search.http` 를 위에서 아래로 실행하면 표의 모든 행이 자동으로 검증됩니다.** 실제 구현이 들어가면 입력별로 기대값이 달라지므로 이 문서를 실제 데이터 기준으로 다시 써야 합니다.
