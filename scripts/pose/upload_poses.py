#!/usr/bin/env python3
"""PM 포즈 데이터 적재 스크립트 (BACKEND-276).

기존 presigned 흐름을 그대로 쓴다.
  POST /api/media/upload (최대 10개) -> 각 파일을 presigned URL 로 PUT -> POST /api/poses/admin/upload (JSON)

진행 상황은 원장(JSON) 파일에 남기므로 같은 명령을 다시 실행해도 결과가 같다.
서버도 sourceKey(DB UNIQUE)로 중복 등록을 막는다 (409 D-18).

사용법과 상태별 의미는 docs/runbook/pose-bulk-upload.md 를 본다.
토큰은 환경변수 NEKI_ADMIN_TOKEN 으로만 받고 출력하지 않는다. python3 표준 라이브러리만 쓴다.
"""

from __future__ import annotations

import argparse
import csv
import datetime as dt
import fnmatch
import json
import os
import re
import sys
import tempfile
import urllib.error
import urllib.request
from pathlib import Path

SOURCE_KEY_PATTERN = re.compile(r"^[A-Za-z0-9._-]{1,100}$")
HEAD_COUNTS = {"ONE", "TWO", "THREE", "FOUR", "FIVE_OR_MORE"}
RELATIONS = {"FRIEND", "COUPLE", "SSUM"}
CONTENT_TYPES = {".png": "image/png", ".jpg": "image/jpeg", ".jpeg": "image/jpeg"}
MAX_BATCH = 10  # /api/media/upload 의 @Size(max = 10)
HTTP_TIMEOUT_SECONDS = 120

CODE_CONFLICT = "D-18"  # CONFLICT_POSE_SOURCE_KEY
CODE_UPLOAD_FAILED = "D-06"  # verify 실패 (S3 에 객체 없음)

# 원장 상태
TICKETED = "TICKETED"  # 티켓(mediaId)만 받음. PUT 여부를 알 수 없으므로 다음 실행에서 새 티켓을 받는다
PUT_DONE = "PUT_DONE"  # S3 PUT 완료. 다음 실행에서 같은 mediaId 를 재사용한다 (같은 ADMIN 계정이어야 한다)
REGISTERED = "REGISTERED"  # 포즈 등록 완료
CONFLICT = "CONFLICT"  # 서버에 같은 sourceKey 가 이미 있음 (완료로 간주). mediaId 는 고아 후보
FAILED = "FAILED"  # 다음 실행에서 새 티켓부터 다시 한다
DONE_STATES = {REGISTERED, CONFLICT}


class AbortRun(Exception):
    """이번 실행을 멈춰야 하는 상황 (통신 예외, 401/403). 원장은 저장한 뒤 멈춘다."""


class HttpResult:
    def __init__(self, status: int, body: bytes):
        self.status = status
        self.body = body

    def json(self) -> dict:
        try:
            return json.loads(self.body.decode("utf-8"))
        except (ValueError, UnicodeDecodeError):
            return {}

    @property
    def result_code(self) -> str | None:
        return self.json().get("resultCode")

    @property
    def ok(self) -> bool:
        return 200 <= self.status < 300


def now() -> str:
    return dt.datetime.now().astimezone().isoformat(timespec="seconds")


class Ledger:
    """media_key -> {status, mediaId?, updatedAt, error?}. 바뀔 때마다 임시 파일에 쓰고 os.replace 로 원자적으로 바꾼다."""

    def __init__(self, path: Path):
        self.path = path
        self.entries: dict[str, dict] = {}
        if path.exists():
            self.entries = json.loads(path.read_text(encoding="utf-8"))

    def get(self, key: str) -> dict | None:
        return self.entries.get(key)

    def set(self, key: str, status: str, media_id: int | None = None, error: str | None = None) -> None:
        entry: dict = {"status": status, "updatedAt": now()}
        if media_id is not None:
            entry["mediaId"] = media_id
        if error:
            entry["error"] = error
        self.entries[key] = entry
        self.save()

    def save(self) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        fd, tmp = tempfile.mkstemp(prefix=".ledger-", dir=str(self.path.parent))
        with os.fdopen(fd, "w", encoding="utf-8") as f:
            json.dump(self.entries, f, ensure_ascii=False, indent=2, sort_keys=True)
        os.replace(tmp, self.path)


class Api:
    def __init__(self, server: str, token: str):
        self.server = server.rstrip("/")
        self.token = token
        self.calls = {"ticket": 0, "put": 0, "admin_upload": 0}

    def _send(self, req: urllib.request.Request) -> HttpResult:
        try:
            with urllib.request.urlopen(req, timeout=HTTP_TIMEOUT_SECONDS) as resp:
                return HttpResult(resp.status, resp.read())
        except urllib.error.HTTPError as e:
            return HttpResult(e.code, e.read())
        except (urllib.error.URLError, OSError) as e:
            # timeout, 연결 끊김. 서버가 요청을 처리했는지 알 수 없으므로 재시도하지 않고 멈춘다.
            # (같은 mediaId 로 동시 재시도하면 서버 경합 롤백이 이미 커밋된 포즈의 media 를 INITIATED 로 되돌릴 수 있다)
            raise AbortRun(f"통신 예외: {type(e).__name__}: {e}") from e

    def _post_json(self, path: str, payload: dict) -> HttpResult:
        req = urllib.request.Request(
            self.server + path,
            data=json.dumps(payload).encode("utf-8"),
            method="POST",
            headers={"Authorization": f"Bearer {self.token}", "Content-Type": "application/json"},
        )
        return self._send(req)

    def tickets(self, items: list[dict]) -> HttpResult:
        self.calls["ticket"] += 1
        return self._post_json("/api/media/upload", {"items": items})

    def put(self, url: str, content_type: str, data: bytes) -> HttpResult:
        self.calls["put"] += 1
        # presigner 가 Content-Type 을 서명하므로 티켓 응답의 contentType 과 정확히 같아야 한다 (다르면 S3 403)
        req = urllib.request.Request(url, data=data, method="PUT", headers={"Content-Type": content_type})
        return self._send(req)

    def admin_upload(self, uploads: list[dict]) -> HttpResult:
        self.calls["admin_upload"] += 1
        return self._post_json("/api/poses/admin/upload", {"uploads": uploads})


def read_csv(path: Path, required: list[str]) -> list[dict]:
    with path.open(encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f)
        header = [h.strip().lower() for h in (reader.fieldnames or [])]
        missing = [c for c in required if c not in header]
        if missing:
            sys.exit(f"{path}: 필수 헤더 없음 {missing}")
        rows = []
        for row in reader:
            if None in row:
                # 헤더보다 열이 많은 행. DictReader 는 남는 값을 None 키의 list 로 담는다
                sys.exit(
                    f"{path}:{reader.line_num}: 열 수가 헤더({len(header)}개)보다 많습니다 "
                    f"({len(header) + len(row[None])}개). 값에 쉼표가 있는지 확인하세요"
                )
            normalized = {(k or "").strip().lower(): (v or "").strip() for k, v in row.items()}
            if any(normalized.values()):
                rows.append(normalized)
        return rows


def to_int(raw: str) -> int | None:
    try:
        return int(raw)
    except (TypeError, ValueError):
        return None


class Row:
    def __init__(self, key: str, head_count: str, relation: str | None, file_name: str, meta: dict):
        self.key = key
        self.head_count = head_count
        self.relation = relation
        self.file_name = file_name
        self.width = to_int(meta.get("width", ""))
        self.height = to_int(meta.get("height", ""))
        self.path: Path | None = None
        self.content_type: str | None = None


def build_plan(args, report: dict) -> list[Row]:
    pose_rows = read_csv(args.pose_csv, ["media_key", "head_count", "tag"])
    media_rows = read_csv(args.media_csv, ["media_key", "file_name"]) if args.media_csv else []
    media_by_key: dict[str, dict] = {}
    for m in media_rows:
        media_by_key.setdefault(m["media_key"], m)

    report["rows"] = len(pose_rows)
    key_counts: dict[str, int] = {}
    for r in pose_rows:
        key_counts[r["media_key"]] = key_counts.get(r["media_key"], 0) + 1

    rows: list[Row] = []
    for r in pose_rows:
        key = r["media_key"]  # 대소문자를 바꾸지 않는다 (서버도 구분한다)
        if args.only and not any(fnmatch.fnmatchcase(key, g) for g in args.only):
            report["filtered_out"] += 1
            continue

        head_count = r["head_count"].upper()
        relation = r["tag"].upper() or None
        reason = None
        if not SOURCE_KEY_PATTERN.match(key):
            reason = "INVALID_KEY"
        elif key_counts[key] > 1:
            reason = "DUPLICATE_KEY_IN_CSV"
        elif head_count not in HEAD_COUNTS:
            reason = "INVALID_HEAD_COUNT"
        elif relation is not None and relation not in RELATIONS:
            reason = "INVALID_RELATION"
        elif relation is not None and head_count != "TWO":
            reason = "RELATION_NOT_ALLOWED"
        if reason:
            report["local_invalid"].append({"key": key, "reason": reason})
            continue

        meta = media_by_key.get(key, {})
        file_name = meta.get("file_name") or f"{key}.png"
        if Path(file_name).name != file_name:
            # 절대경로, ../, 경로 구분자가 있으면 --images-dir 밖의 파일을 읽을 수 있으므로 거른다
            report["local_invalid"].append({"key": key, "reason": "INVALID_FILE_NAME"})
            continue
        row = Row(key, head_count, relation, file_name, meta)
        row.path = next((d / row.file_name for d in args.images_dir if (d / row.file_name).is_file()), None)
        if row.path is None:
            report["no_image"].append(key)
            continue
        row.content_type = CONTENT_TYPES.get(row.path.suffix.lower())
        if row.content_type is None:
            report["local_invalid"].append({"key": key, "reason": "UNSUPPORTED_EXTENSION"})
            continue
        rows.append(row)
    return rows


def process_batch(batch: list[Row], api: Api, ledger: Ledger, report: dict) -> None:
    # a. PUT_DONE 은 같은 mediaId 를 다시 쓴다. verify 는 S3 존재만 보므로 티켓 만료와 무관하다
    media_ids: dict[str, int] = {}
    need_ticket: list[Row] = []
    for row in batch:
        entry = ledger.get(row.key) or {}
        if entry.get("status") == PUT_DONE and entry.get("mediaId"):
            media_ids[row.key] = entry["mediaId"]
        else:
            if entry.get("status") in (TICKETED, FAILED) and entry.get("mediaId"):
                report["orphan_media_ids"].append({"key": row.key, "mediaId": entry["mediaId"], "why": "superseded"})
            need_ticket.append(row)

    # b. 티켓 발급 (응답 items 는 요청 순서와 같다)
    if need_ticket:
        items = [
            {
                "filename": row.path.name,
                "contentType": row.content_type,
                "mediaType": "POSE",
                "width": row.width,
                "height": row.height,
                "size": row.path.stat().st_size,
            }
            for row in need_ticket
        ]
        res = api.tickets(items)
        if res.status in (401, 403):
            raise AbortRun(f"티켓 발급 {res.status} (토큰 확인)")
        tickets = (res.json().get("data") or {}).get("items") or []
        if not res.ok or len(tickets) != len(need_ticket):
            for row in need_ticket:
                ledger.set(row.key, FAILED, error=f"ticket HTTP {res.status} {res.result_code}")
                report["failed"].append({"key": row.key, "reason": f"ticket HTTP {res.status} {res.result_code}"})
        else:
            for row, ticket in zip(need_ticket, tickets):
                ledger.set(row.key, TICKETED, media_id=ticket["mediaId"])
                # c. PUT
                put = api.put(ticket["uploadTicket"], ticket["contentType"], row.path.read_bytes())
                if put.ok:
                    ledger.set(row.key, PUT_DONE, media_id=ticket["mediaId"])
                    media_ids[row.key] = ticket["mediaId"]
                else:
                    ledger.set(row.key, FAILED, media_id=ticket["mediaId"], error=f"PUT HTTP {put.status}")
                    report["failed"].append({"key": row.key, "reason": f"PUT HTTP {put.status}"})
                    report["orphan_media_ids"].append(
                        {"key": row.key, "mediaId": ticket["mediaId"], "why": "put_failed"}
                    )

    ready = [row for row in batch if row.key in media_ids]
    if not ready:
        return

    # d. 포즈 등록 (CSV 순서 = 같은 배치 안에서는 마지막 행이 목록 맨 위)
    def upload_item(row: Row) -> dict:
        return {
            "mediaId": media_ids[row.key],
            "headCount": row.head_count,
            "memo": None,
            "relation": row.relation,
            "sourceKey": row.key,
        }

    res = api.admin_upload([upload_item(row) for row in ready])
    if res.ok:
        for row in ready:
            ledger.set(row.key, REGISTERED, media_id=media_ids[row.key])
            report["registered"].append(row.key)
        return
    if res.status in (401, 403):
        raise AbortRun(f"admin/upload {res.status} (토큰 또는 ADMIN 권한 확인)")

    # 응답을 받은 실패(409 포함)는 서버 처리가 끝난 것이므로 1건씩 다시 보내 어느 키가 문제인지 가린다
    for row in ready:
        single = api.admin_upload([upload_item(row)])
        media_id = media_ids[row.key]
        if single.ok:
            ledger.set(row.key, REGISTERED, media_id=media_id)
            report["registered"].append(row.key)
        elif single.status in (401, 403):
            raise AbortRun(f"admin/upload {single.status} (토큰 또는 ADMIN 권한 확인)")
        elif single.status == 409 and single.result_code == CODE_CONFLICT:
            ledger.set(row.key, CONFLICT, media_id=media_id)
            report["conflict"].append({"key": row.key, "orphanMediaId": media_id})
        elif single.result_code == CODE_UPLOAD_FAILED:
            # S3 에 객체가 없다. 다음 실행에서 새 티켓부터 다시 한다
            error = f"admin/upload HTTP {single.status} {single.result_code}"
            ledger.set(row.key, FAILED, media_id=media_id, error=error)
            report["failed"].append({"key": row.key, "reason": error + " (다음 실행에서 새 티켓)"})
        else:
            # PUT 은 끝났으므로 다음 실행에서 같은 mediaId 를 다시 쓴다 (고아를 만들지 않는다)
            error = f"admin/upload HTTP {single.status} {single.result_code}"
            ledger.set(row.key, PUT_DONE, media_id=media_id, error=error)
            report["failed"].append({"key": row.key, "reason": error + " (다음 실행에서 같은 mediaId 재사용)"})


def main() -> int:
    parser = argparse.ArgumentParser(description="PM 포즈 데이터 적재 (presigned 흐름 + /api/poses/admin/upload)")
    parser.add_argument("--server", required=True, help="예: http://localhost:8080")
    parser.add_argument("--pose-csv", required=True, type=Path)
    parser.add_argument("--media-csv", type=Path)
    parser.add_argument("--images-dir", required=True, action="append", type=Path)
    parser.add_argument("--ledger", required=True, type=Path, help="진행 원장 JSON. 레포 밖 경로 권장")
    parser.add_argument("--only", action="append", help="media_key glob (여러 번 가능)")
    parser.add_argument("--batch-size", type=int, default=MAX_BATCH)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    if not 1 <= args.batch_size <= MAX_BATCH:
        parser.error(f"--batch-size 는 1~{MAX_BATCH}")

    report: dict = {
        "rows": 0,
        "filtered_out": 0,
        "local_invalid": [],
        "no_image": [],
        "already_in_ledger": 0,
        "pending": 0,
        "batches": 0,
        "registered": [],
        "conflict": [],
        "failed": [],
        "orphan_media_ids": [],
        "aborted": None,
    }

    rows = build_plan(args, report)
    ledger = Ledger(args.ledger)
    pending = []
    for row in rows:
        if (ledger.get(row.key) or {}).get("status") in DONE_STATES:
            report["already_in_ledger"] += 1
        else:
            pending.append(row)
    batches = [pending[i : i + args.batch_size] for i in range(0, len(pending), args.batch_size)]
    report["pending"] = len(pending)
    report["batches"] = len(batches)

    if args.dry_run:
        report["plan"] = [[row.key for row in b] for b in batches]
        print(json.dumps(summarize(report, dry_run=True), ensure_ascii=False, indent=2))
        return 0

    token = os.environ.get("NEKI_ADMIN_TOKEN", "")
    if not token:
        sys.exit("환경변수 NEKI_ADMIN_TOKEN 이 비어 있습니다")
    api = Api(args.server, token)

    try:
        for batch in batches:
            process_batch(batch, api, ledger, report)
    except AbortRun as e:
        report["aborted"] = str(e)
    finally:
        ledger.save()

    report["api_calls"] = api.calls
    print(json.dumps(summarize(report, dry_run=False), ensure_ascii=False, indent=2))
    if report["aborted"]:
        return 2
    return 1 if report["failed"] else 0


def summarize(report: dict, dry_run: bool) -> dict:
    out = {
        "rows": report["rows"],
        "filtered_out": report["filtered_out"],
        "local_invalid": report["local_invalid"],
        "no_image": {"count": len(report["no_image"]), "keys": report["no_image"]},
        "already_in_ledger": report["already_in_ledger"],
        "pending": report["pending"],
        "batches": report["batches"],
    }
    if dry_run:
        out["plan"] = report["plan"]
        return out
    out.update(
        {
            "registered": len(report["registered"]),
            "conflict": report["conflict"],
            "failed": report["failed"],
            "orphan_media_ids": report["orphan_media_ids"],
            "api_calls": report.get("api_calls"),
            "aborted": report["aborted"],
        }
    )
    return out


if __name__ == "__main__":
    sys.exit(main())
