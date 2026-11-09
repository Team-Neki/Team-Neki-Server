# 포즈 일괄 업로드 (PM 용, BACKEND-276)

PM 이 `pose.csv`, `media.csv`, 이미지 파일로 포즈를 직접 등록하는 절차입니다. 서버가 이미지를 S3 에 올리고
media 와 포즈를 행 단위로 만듭니다. 같은 `media_key` 는 한 번만 등록되므로 같은 파일을 다시 보내도 안전합니다.

## 준비

- ADMIN 권한 계정의 access token. 없으면 서버 담당에게 요청합니다.
- `pose.csv` : 헤더 `media_key,head_count,tag`. `tag` 는 `friend` / `couple` / `ssum` 또는 빈 값입니다.
  - 관계(tag)는 2인(`TWO`) 포즈에만 붙일 수 있습니다. 1인 포즈의 tag 는 비워 둡니다.
  - `media_key` 는 영문, 숫자, `.`, `_`, `-` 만 쓰고 100자 이하입니다.
  - 데이터 행은 1~1000개입니다. 값에 따옴표(`"`)를 쓰지 않습니다.
- `media.csv` (선택) : 헤더 `media_key,file_name` 이 있어야 합니다. 나머지 열(width, height, size)은 읽지 않습니다.
  서버가 이미지 파일에서 형식과 크기를 직접 읽습니다. `media.csv` 를 빼면 `media_key + .png` (`.jpg`, `.jpeg`) 로 이미지를 찾습니다.
- 이미지 : PNG 또는 JPEG, 파일당 10MB 이하.

## 한도와 나누는 기준

| 항목 | 한도 | 넘으면 |
|---|---|---|
| 파일 하나 | 10MB | 413 `D-17` |
| 요청 전체 | 128MB | 413 `D-17` (또는 응답 없이 연결이 끊김) |
| part 수 (CSV 2개 + 이미지) | 100개 | 413 `D-18` |

- 이미지가 약 2MB 이면 한 번에 최대 약 50~60장입니다. friend 51장(약 101MiB)은 한 번에 올라갑니다.
- 운영 Traefik 은 요청을 60초 안에 받아야 합니다. 128MB 를 60초에 보내려면 업로드 속도가 약 18Mbps 이상이어야 합니다.
  연결이 끊기거나 502, timeout 이 나면 요청당 약 25장(약 50MB)으로 나눠 다시 올립니다.
- 용량을 넘으면 서버는 본문을 다 받기 전에 413 `D-17` 을 돌려줍니다(로컬 실측: 159MiB 요청에 약 1.9MB 만 보낸 뒤 413 JSON 수신.
  multipart 는 이 엔드포인트의 핸들러가 인자를 풀 때만 파싱하며(`resolve-lazily: true`), 이 상태에서 측정한 값).
  네트워크 구간(Traefik 등)에 따라 413 대신 연결이 그냥 끊긴 것처럼 보일 수도 있습니다.
  어느 쪽이든 아무것도 저장되지 않았으니 나눠서 다시 올리면 됩니다.
- 매번 `pose.csv` 전체를 같이 보내도 됩니다. 이미지가 없는 행은 `IMAGE_NOT_PROVIDED` 로 건너뜁니다.

## 올리기 (curl)

운영은 Swagger 가 꺼져 있으므로 curl 을 씁니다. 이미지 폴더에서 실행합니다.

```bash
SERVER=https://yapp.suitestudy.com:4641          # staging: https://dev-yapp.suitestudy.com:4641
TOKEN=...                                         # ADMIN access token (공유하거나 커밋하지 말 것)
CSV_DIR=~/Downloads                               # pose.csv, media.csv 위치

args=(); for f in *.png; do args+=(-F "images=@$f;type=image/png"); done
curl -sS -X POST "$SERVER/api/poses/admin/upload" \
  -H "Authorization: Bearer $TOKEN" \
  -F "poseCsv=@$CSV_DIR/pose.csv;type=text/csv" \
  -F "mediaCsv=@$CSV_DIR/media.csv;type=text/csv" \
  "${args[@]}" | jq '.data.summary, .data.failed, .data.unmatchedImages'
```

나눠 올릴 때는 `for f in $(ls *.png | sed -n '1,25p')` 처럼 범위를 바꿔 여러 번 실행합니다.

## 응답 읽기

```json
{ "summary": { "totalRows": 153, "created": 51, "skipped": 102, "failed": 0, "unmatchedImages": 0 },
  "created": [ { "sourceKey": "friend_001_F1-4_F2-4", "poseId": 1, "mediaId": 1, "headCount": "TWO", "relation": "FRIEND" } ],
  "skipped": [ { "sourceKey": "couple_001_F1-4_M1-5", "line": 2, "reason": "IMAGE_NOT_PROVIDED" } ],
  "failed": [],
  "unmatchedImages": [] }
```

- `line` 은 pose.csv 의 줄 번호입니다(헤더가 1).
- `skipped` 는 정상입니다. 다시 보내도 결과가 같습니다.
  - `ALREADY_REGISTERED` : 이미 등록된 media_key
  - `IMAGE_NOT_PROVIDED` : 이번 요청에 그 행의 이미지가 없음
- `failed` 는 고친 뒤 다시 보내야 합니다.

| reason | 뜻 | 조치 |
|---|---|---|
| `INVALID_KEY` | media_key 형식 오류 | 영문/숫자/`._-`, 100자 이하로 수정 |
| `DUPLICATE_KEY_IN_CSV` | pose.csv 에 같은 media_key 가 두 번 이상 | 중복 행 정리 |
| `INVALID_HEAD_COUNT` | head_count 가 ONE/TWO/THREE/FOUR/FIVE_OR_MORE 가 아님 | 값 수정 |
| `INVALID_RELATION` | tag 가 friend/couple/ssum/빈 값이 아님 | 값 수정 |
| `RELATION_NOT_ALLOWED` | 2인이 아닌데 tag 가 있음 | tag 를 비우거나 head_count 확인 |
| `DUPLICATE_IMAGE` | 한 행에 맞는 이미지가 둘 이상 (예: a.png 와 a.jpg) | 하나만 남김 |
| `IMAGE_SHARED_BY_ROWS` | 이미지 하나가 두 행 이상에 해당 (media.csv 의 여러 키가 같은 file_name, 또는 대소문자만 다른 media_key) | media.csv 의 file_name 이나 media_key 를 행마다 다르게 고침 |
| `UNSUPPORTED_IMAGE` | PNG/JPEG 가 아니거나 헤더가 깨짐 | 파일 다시 내보내기 |
| `STORAGE_FAILED`, `SAVE_FAILED` | 서버 쪽 일시 오류 | 같은 요청을 다시 보냄 (등록된 행은 건너뜀) |

- 한 행에 여러 사유가 겹치면 앞 단계 사유 하나만 나옵니다. 판정 순서는
  `INVALID_KEY` → `DUPLICATE_KEY_IN_CSV` → `ALREADY_REGISTERED` → `INVALID_HEAD_COUNT` → `INVALID_RELATION`/`RELATION_NOT_ALLOWED`
  → `IMAGE_NOT_PROVIDED` → `DUPLICATE_IMAGE` → `IMAGE_SHARED_BY_ROWS` 입니다.
  예: 같은 이미지를 가리키는 두 행 중 한 행이 이미 등록돼 있으면 그 행은 `ALREADY_REGISTERED`, 다른 행은 `IMAGE_SHARED_BY_ROWS` 입니다.
- `unmatchedImages` : 어느 행에도 해당하지 않는 이미지 파일명입니다. 파일명이나 media.csv 의 file_name 을 확인합니다.
- 오류 응답: 400 `D-01` (poseCsv 누락, CSV 헤더 누락, 따옴표, 0행/1000행 초과), 413 `D-17`/`D-18` (위 한도), 403 (ADMIN 아님).

## couple 이미지를 나중에 받았을 때

1. 받은 이미지를 한 폴더에 둡니다. 파일명은 `media_key.png` 이거나 media.csv 의 file_name 과 같아야 합니다.
2. 같은 `pose.csv`, `media.csv` 와 함께 위 curl 을 실행합니다. couple 72장(약 145MiB)은 128MB 를 넘으므로 2~3번으로 나눕니다.
3. 이미 등록된 friend/ssum/solo 는 `ALREADY_REGISTERED` 로 건너뛰고 couple 만 등록됩니다.

## 참고

- 한 요청 안에서는 pose.csv 순서대로 등록하므로 CSV 마지막 행이 앱 목록 맨 위에 옵니다.
- JPEG 는 파일에 저장된 가로·세로를 그대로 씁니다. EXIF 회전(orientation 6/8)이 있는 사진은 가로·세로가 바뀌어 기록되므로
  회전을 적용해 다시 내보낸 파일을 올립니다. PM 데이터는 전부 PNG 라 해당 없습니다.

## 서버 담당 메모

- **이 브랜치를 단독으로 staging 에 배포하지 않는다.** V46 은 BACKEND-83 계열(V41~V45)이 main 에 먼저 머지된다는 전제다.
  staging/prod 는 `out-of-order: false` 라 V46 이 먼저 적용되면 나중에 오는 V44, V45 가 거부된다.
  `deploy-api-staging.yml` 은 임의 ref 를 dispatch 할 수 있으므로 BACKEND-83 계열 머지 전에는 이 브랜치로 실행하지 않는다.
- 로컬 DB 에 V46 만 먼저 적용하면 나중에 BACKEND-83 브랜치를 띄울 때 V41~V45 가 `ignored` 로 validate 에 실패한다.
  그때는 `-Dspring.flyway.out-of-order=true` 로 한 번 실행하거나 로컬 DB 를 다시 만든다.
- multipart 한도(10MB/128MB)는 전역 설정이지만 `spring.servlet.multipart.resolve-lazily: true` 라서 본문 파싱은
  `@RequestPart` 를 쓰는 핸들러(현재 `bulkUploadPoses` 하나)에서만 일어난다. 다른 경로로 온 multipart 본문은 미리 파싱하지 않는다.
  part 수 한도(`server.tomcat.max-part-count: 100`)는 Tomcat 커넥터 설정이며 파싱할 때 적용된다.
- 보상(discard)이 실패하면 `[POSE_BULK_UPLOAD] discard failed, orphan mediaId=... key=...` error 로그가 남는다.
  해당 media 는 UPLOADED 이지만 포즈에 연결되지 않아 앱에 노출되지 않는다.
