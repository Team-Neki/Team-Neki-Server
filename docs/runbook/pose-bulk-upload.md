# 포즈 일괄 적재 (PM 데이터, BACKEND-276)

PM 이 준 `pose.csv`, `media.csv`, 이미지 폴더를 `scripts/pose/upload_poses.py` 로 적재하는 절차입니다.
스크립트는 앱과 같은 presigned 흐름을 씁니다.

1. `POST /api/media/upload` 로 업로드 티켓을 받습니다. 한 번에 최대 10개입니다.
2. 받은 presigned URL 로 S3 에 PUT 합니다.
3. `POST /api/poses/admin/upload` (JSON) 로 포즈를 등록합니다.

`media_key` 는 `sourceKey` 로 저장되고 DB UNIQUE 입니다. 같은 키는 한 번만 등록됩니다.
진행 상황은 원장(JSON) 파일에 남으므로 같은 명령을 다시 실행해도 결과가 같습니다.

## 사전 조건

- python3 (표준 라이브러리만 씀)
- ADMIN 권한 계정의 access token
  - 환경변수 `NEKI_ADMIN_TOKEN` 으로만 넘깁니다. 인자로 넘기면 셸 히스토리와 `ps` 에 남습니다.
  - 토큰은 공유하거나 커밋하지 않습니다.
  - **재실행은 처음과 같은 ADMIN 계정으로 합니다.** 원장의 mediaId 는 처음 계정 소유입니다. 다른 계정으로 재실행하면 서버의 업로드 확인(owner 일치)에서 찾지 못해 실패합니다.
- 입력 파일
  - `pose.csv`: 헤더 `media_key,head_count,tag`. `tag` 는 `friend`/`couple`/`ssum` 또는 빈 값이고, 관계는 `TWO` 에만 붙일 수 있습니다.
  - `media.csv`(선택): 헤더 `media_key,file_name` 이 필수이고, `width,height` 가 있으면 함께 보냅니다. 없으면 `media_key.png` 로 찾습니다.
  - 이미지: PNG/JPEG. 확장자로 Content-Type 을 정합니다.
- `media_key` 는 CSV 에 적힌 그대로 씁니다. 영문, 숫자, `.`, `_`, `-` 로 1~100자이고 대소문자를 구분합니다. `Friend_001` 과 `friend_001` 은 다른 키로 등록되므로 대소문자를 바꾸지 않습니다.

## 환경별 서버

| 환경 | `--server` |
|---|---|
| local | `http://localhost:8080` |
| staging | `https://dev-yapp.suitestudy.com:4641` |
| prod | `https://yapp.suitestudy.com:4641` |

## 실행

```bash
export NEKI_ADMIN_TOKEN=...          # 출력하지 말 것
LEDGER=~/neki-ledger/pose-prod.json  # 레포 밖, 환경별로 따로 둔다

# 1) 계획만 확인 (네트워크 없음)
python3 scripts/pose/upload_poses.py --server https://yapp.suitestudy.com:4641 \
  --pose-csv ~/Downloads/pose.csv --media-csv ~/Downloads/media.csv \
  --images-dir ~/Downloads/neki_pose_all --images-dir "$HOME/Downloads/neki_pose_all 2" \
  --ledger "$LEDGER" --dry-run

# 2) 적재: 같은 명령에서 --dry-run 을 뺀다
```

- `--only 'friend_00[12]_*'` 처럼 glob 으로 일부 키만 처리할 수 있습니다. 여러 번 줄 수 있습니다. staging 에서 소량으로 먼저 확인할 때 씁니다.
- `--batch-size` 는 기본값과 최댓값이 10 입니다.
- 원장 파일은 환경마다 따로 두고 지우지 않습니다. 커밋하지 않습니다.

## 결과 읽기

스크립트는 JSON 요약을 출력합니다. 종료코드는 0(정상), 1(failed 있음), 2(중단)입니다.

| 항목 | 뜻 |
|---|---|
| `rows` | pose.csv 데이터 행 수 |
| `local_invalid` | 서버에 보내기 전에 거른 행 (`INVALID_KEY`, `DUPLICATE_KEY_IN_CSV`, `INVALID_HEAD_COUNT`, `INVALID_RELATION`, `RELATION_NOT_ALLOWED`, `UNSUPPORTED_EXTENSION`) |
| `no_image` | 이미지 파일이 없어 건너뛴 키 (예: 아직 받지 않은 couple). 파일이 생기면 재실행으로 처리됩니다 |
| `already_in_ledger` | 원장에 `REGISTERED`/`CONFLICT` 로 남아 건너뛴 키 |
| `registered` | 이번에 등록한 수 |
| `conflict` | 서버에 같은 sourceKey 가 이미 있던 키와, 그때 새로 받은 mediaId(고아 후보) |
| `failed` | 실패한 키와 사유. 재실행하면 다시 시도합니다 |
| `orphan_media_ids` | 이번 실행에서 버려진 mediaId (PUT 실패, 새 티켓으로 대체) |
| `api_calls` | ticket / put / admin_upload 호출 수 |
| `aborted` | 실행을 멈춘 사유 (통신 예외, 401/403) |

원장 상태와 재실행 규칙

| 상태 | 뜻 | 재실행하면 |
|---|---|---|
| `TICKETED` | 티켓만 받고 PUT 결과를 모름 | 새 티켓을 받습니다. 이전 mediaId 는 고아 후보입니다 |
| `PUT_DONE` | S3 PUT 완료 | 같은 mediaId 로 등록만 다시 합니다. 티켓이 만료돼도 됩니다 |
| `REGISTERED` | 등록 완료 | 건너뜁니다 |
| `CONFLICT` | 서버에 같은 키가 이미 있음 (완료로 간주) | 건너뜁니다 |
| `FAILED` | 티켓 발급 실패, PUT 실패, 또는 등록 시 `D-06`(S3 에 객체 없음) | 새 티켓부터 다시 합니다 |

등록 요청 처리 규칙
- 배치 등록이 200 이면 그 배치는 모두 `REGISTERED` 입니다.
- 배치 등록이 응답을 받은 채 실패하면(409 포함) 1건씩 다시 보내 문제 키를 가립니다.
  - 409 이면서 `resultCode` 가 `D-18` 이면 `CONFLICT` 입니다.
  - `D-06` 이면 `FAILED` 입니다.
  - 그 밖의 실패는 `PUT_DONE` 을 유지하고 다음 실행에서 같은 mediaId 를 씁니다.
- **응답이 없으면(timeout 120초, 연결 끊김) 재시도하지 않고 원장을 저장한 뒤 멈춥니다.** 서버가 첫 요청을 아직 처리하고 있을 수 있기 때문입니다. 같은 mediaId 로 바로 다시 보내면 서버의 경합 롤백이 이미 커밋된 포즈의 media 를 INITIATED 로 되돌려 목록에서 사라지게 할 수 있습니다. 잠시 뒤 같은 명령으로 재실행하면 이미 커밋된 키는 409 로 확인됩니다.
- 401/403 이면 토큰이나 권한 문제이므로 바로 멈춥니다.

## 서버 계약 (`POST /api/poses/admin/upload`)

- `uploads[].relation`(선택), `uploads[].sourceKey`(선택)
- 400 `D-01`: mediaId 중복, TWO 가 아닌데 relation 이 있음, sourceKey 형식 오류, 요청 안 sourceKey 중복
- 409 `D-18`: 이미 등록된 sourceKey 가 하나라도 있으면 요청 전체가 실패합니다(all-or-nothing). 포즈는 만들지 않습니다.
  - 사전 검사에서 걸리면 media 상태는 바뀌지 않습니다(INITIATED).
  - 저장 시 경합이면 기존 보상 로직으로 INITIATED 로 되돌립니다.
  - 응답에는 키 목록이 없습니다. 서버 WARN 로그 `[POSE_SOURCE_KEY_CONFLICT] keys=[...]` 에 남습니다.

## couple 이미지를 받았을 때

1. 받은 이미지를 새 폴더에 둡니다. 파일명은 media.csv 의 `file_name`(또는 `media_key.png`)과 같아야 합니다.
2. 처음과 같은 명령에 `--images-dir <새 폴더>` 만 추가해 같은 원장으로 실행합니다.
3. 이미 등록된 키는 원장에서 건너뛰고(`already_in_ledger`), couple 만 등록됩니다.

## 실행 후 점검

적재(또는 재실행)를 마치면 아래를 차례로 확인합니다.

1. 스크립트 요약에서 `failed` 가 비어 있고 `aborted` 가 null 인지 봅니다. 중단됐다면 같은 원장으로 재실행을 먼저 끝냅니다.
2. 포즈는 있는데 media 가 UPLOADED 가 아닌 행이 0건인지 봅니다 (아래 "포즈는 있는데 media 가 UPLOADED 가 아닌 행").
3. 고아 media 를 확인합니다 (아래 "고아 media 확인과 정리").

## 실패하면 확인할 것

- PUT 이 403 이면 Content-Type 불일치나 티켓 만료(10분)일 수 있습니다. 스크립트는 티켓 응답의 contentType 을 그대로 쓰므로, 대개 티켓을 받고 10분 넘게 지난 경우입니다. 재실행하면 새 티켓을 받습니다.
- 등록이 400 `D-06` 이면 S3 에 객체가 없는 경우입니다. 재실행하면 새 티켓부터 다시 합니다.
- 등록이 400 `D-04` 등으로 계속 실패하면 다른 ADMIN 계정으로 실행하지 않았는지 확인합니다.

## 포즈는 있는데 media 가 UPLOADED 가 아닌 행

앱 목록은 UPLOADED media 만 보여주므로, 이런 행의 포즈는 등록돼 있어도 앱에 보이지 않습니다.

**원인**
- timeout 으로 중단한 뒤 너무 빨리 재실행하면 생길 수 있습니다.
- 첫 요청이 서버에서 아직 처리 중일 때 같은 mediaId 로 두 번째 요청이 들어오면, 두 번째 요청이 UNIQUE 경합으로 실패합니다.
- 그 실패를 보상하는 롤백이 첫 요청이 커밋한 포즈의 media 를 INITIATED 로 되돌립니다.
- 스크립트는 통신 예외에서 재시도하지 않고 멈추므로, 사람이 바로 다시 실행할 때만 생깁니다. 중단 뒤에는 잠시(서버 처리가 끝날 만큼) 기다렸다가 재실행합니다.

**확인**
```sql
SELECT p.id, p.source_key, m.id, m.status
FROM tb_pose p JOIN tb_media m ON m.id = p.media_id
WHERE m.media_type = 'POSE' AND m.status <> 'UPLOADED';
```

**복구** (행이 나왔을 때)
1. 행마다 storage_key 를 뽑아 S3 에 객체가 있는지 확인합니다.
   ```bash
   psql "$DB_URL" -qAt -c "SELECT m.id, m.storage_key FROM tb_pose p JOIN tb_media m ON m.id = p.media_id
     WHERE m.media_type = 'POSE' AND m.status <> 'UPLOADED';" |
   while IFS='|' read -r id key; do
     aws s3api head-object --bucket "$BUCKET" --key "$key" >/dev/null 2>&1 && echo "$id exists" || echo "$id MISSING"
   done
   ```
2. 객체가 있는 행만 UPLOADED 로 되돌립니다. 포즈가 참조하고 있고 아직 INITIATED 인 행만 바꿉니다.
   ```sql
   UPDATE tb_media m SET status = 'UPLOADED', updated_at = now()
   WHERE m.id IN (/* exists 로 확인한 id */)
     AND m.media_type = 'POSE'
     AND m.status = 'INITIATED'
     AND EXISTS (SELECT 1 FROM tb_pose p WHERE p.media_id = m.id);
   ```
3. 객체가 없는(MISSING) 행은 이미지가 없는 포즈입니다. 자동으로 고치지 말고 서버 담당과 확인합니다.
   - 처리 방법 1: 그 포즈를 지운 뒤 원장에서 해당 키를 빼고 재실행합니다.
   - 처리 방법 2: 같은 파일을 그 storage_key 로 다시 올립니다.
4. 확인 쿼리를 다시 실행해 0건인지 봅니다.

## 고아 media 확인과 정리

고아 media 는 포즈가 참조하지 않는 POSE media 입니다. 생기는 경우는 다음과 같습니다.
- 티켓만 받고 멈춤
- PUT 실패
- 409 `CONFLICT`
- 원장 없이 재실행
- 통신 예외로 중단

PUT 까지 끝났다면 S3 객체도 남습니다. 앱 목록은 UPLOADED media 만 보여주므로 노출되지는 않습니다. 레포에는 이를 정리하는 배치가 없습니다.

**확인** (INITIATED 와 UPLOADED 모두)
```sql
SELECT m.id, m.status, m.owner_id, m.storage_key, m.created_at
FROM tb_media m
WHERE m.media_type = 'POSE'
  AND m.status IN ('INITIATED', 'UPLOADED')
  AND NOT EXISTS (SELECT 1 FROM tb_pose p WHERE p.media_id = m.id)
ORDER BY m.id;
```
- UPLOADED 인데 포즈가 없는 행은 보상 실패 같은 이상 상황입니다. 자동으로 지우지 말고 따로 확인합니다.

**정리** (INITIATED 만, DB 를 먼저 처리)
- **중단된 원장이 남아 있으면 먼저 같은 원장으로 재실행을 끝낸 뒤 정리합니다.** 원장의 `PUT_DONE` mediaId 는 재실행에서 다시 쓰이는데, 그 전에 지우면 재실행이 실패합니다. 재실행 결과 `aborted` 가 null 이고 `failed` 가 비어 있을 때 정리합니다.
- 스크립트가 보고한 mediaId 를 그대로 믿지 않습니다. 같은 조건으로 다시 걸러서 지웁니다.
- 진행 중인 업로드를 지우지 않도록 1시간 넘은 행만 대상으로 합니다.

```bash
set -euo pipefail
# 1) DB: 조건에 맞는 행만 지우고, 지운 storage_key 를 파일로 받는다 (-q 로 BEGIN/DELETE/COMMIT 태그를 숨김)
psql "$DB_URL" -v ON_ERROR_STOP=1 -qAt > orphan_keys.txt <<'SQL'
BEGIN;
DELETE FROM tb_media m
WHERE m.media_type = 'POSE'
  AND m.status = 'INITIATED'
  AND m.created_at < now() - interval '1 hour'
  AND NOT EXISTS (SELECT 1 FROM tb_pose p WHERE p.media_id = m.id)
RETURNING m.storage_key;
COMMIT;
SQL
wc -l < orphan_keys.txt
# 2) S3: DB 에서 지운 키만 지운다 (pose/ 접두만)
{ grep '^pose/' orphan_keys.txt || true; } | while read -r k; do aws s3 rm "s3://$BUCKET/$k"; done
```
- 지울 키가 0개여도 `|| true` 덕분에 `set -e` 에서 실패하지 않습니다.
- INITIATED 이고 PUT 전이면 S3 객체가 없습니다. 이때 `s3 rm` 은 오류 없이 넘어갑니다.

## 서버 담당 메모

- 이 브랜치는 BACKEND-83 계열보다 먼저 main 에 머지됩니다. 그래서 마이그레이션은 main 바로 다음 버전인 V41 입니다.
  - BACKEND-83 계열은 이 브랜치 머지 후 리베이스해 V42 이후로 번호를 옮깁니다.
  - staging/prod 는 `out-of-order: false` 입니다. 다른 브랜치를 dispatch 해 staging 에 V41 이상이 이미 적용되어 있으면 이 브랜치의 V41 과 충돌하므로 배포 전에 `flyway_schema_history` 를 확인합니다.
- 로컬 DB 에 다른 브랜치의 V41 이상이 적용되어 있으면 validate 에 실패합니다. 로컬 DB 를 다시 만듭니다.
- V41 의 `source_key` 컬럼 COMMENT 는 v2(일괄 업로드) 기준 문구입니다. 적용된 DB 의 checksum 이 깨지므로 SQL 은 고치지 않습니다. 실제 의미는 `Pose.kt` 주석을 따릅니다.
- 한 요청 안에서는 CSV 순서대로 INSERT 하므로 CSV 마지막 행이 앱 목록 맨 위에 옵니다.
