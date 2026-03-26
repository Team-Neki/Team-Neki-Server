---
name: resolve-review
description: GitHub PR의 코드 리뷰 코멘트를 읽어와 피드백을 반영하고, 각 코멘트에 답글을 남깁니다.
---

# Resolve Review Skill

GitHub PR에 남겨진 코드 리뷰 코멘트를 확인하고, 피드백을 코드에 반영한 뒤, 각 코멘트에 처리 결과를 답글로 남깁니다.

---

## 사용법

### 사용 시점

PR에 코드 리뷰 코멘트가 달린 후, 피드백을 반영할 때 사용합니다.

### 호출 방법

```
/resolve-review <PR번호>
```

### 동작 흐름

1. PR의 리뷰 코멘트 조회
2. 각 코멘트의 우선순위와 내용 분석
3. 해당 PR 브랜치로 체크아웃 (worktree 포함)
4. 피드백을 코드에 반영
5. 변경 사항 커밋 및 푸시
6. 각 리뷰 코멘트에 처리 결과 답글 작성

---

## 상세 절차

### Step 1: 리뷰 코멘트 조회

```bash
# PR 리뷰 요약 확인
gh pr view <PR번호> --comments --json comments,reviews \
  --jq '.reviews[] | "[\(.author.login)] state: \(.state)\n\(.body)"'

# 인라인 리뷰 코멘트 조회 (comment ID, 파일 경로, 내용)
gh api repos/{owner}/{repo}/pulls/<PR번호>/comments \
  --jq '.[] | "\(.id)\t\(.path):\(.original_line)\t\(.body)"'
```

### Step 2: 코멘트 분석

각 코멘트에서 다음을 파악:

- **우선순위**: high, medium, low (뱃지 이미지 또는 텍스트에서 추출)
- **대상 파일 및 라인**: `path`와 `line` 필드
- **제안 내용**: `suggestion` 코드 블록 또는 본문 설명
- **이미 답글이 달렸는지**: `in_reply_to_id`가 있는 코멘트는 답글이므로 원본만 처리

### Step 3: 브랜치 체크아웃

```bash
# PR의 head 브랜치 확인
gh pr view <PR번호> --json headRefName --jq '.headRefName'

# 해당 브랜치로 체크아웃 (worktree가 있으면 해당 경로에서 작업)
git checkout <branch>
```

### Step 4: 코드 수정

- 리뷰 코멘트의 suggestion이 있으면 해당 내용을 우선 반영
- suggestion이 없으면 코멘트의 설명을 바탕으로 적절히 수정
- high priority 코멘트를 먼저 처리

### Step 5: 커밋 및 푸시

```bash
git add <수정된 파일들>
git commit -m "refactor: 코드 리뷰 피드백 반영

Co-Authored-By: Claude <model> <noreply@anthropic.com>"
git push
```

### Step 6: 리뷰 코멘트에 답글

각 코멘트에 처리 결과를 답글로 남김:

```bash
gh api repos/{owner}/{repo}/pulls/<PR번호>/comments -X POST \
  -f body="반영 완료했습니다. <변경 요약> (<commit-hash>)" \
  -F in_reply_to=<comment_id>
```

---

## 규칙

- **원본 코멘트만 처리**: `in_reply_to_id`가 null인 코멘트만 대상으로 함 (답글은 무시)
- **우선순위 순서**: high → medium → low 순으로 처리
- **판단이 필요한 경우**: 코멘트의 제안이 프로젝트 컨벤션과 충돌하거나 부적절하다고 판단되면, 사용자에게 확인을 요청
- **답글 형식**: 간결하게 무엇을 변경했는지 + 커밋 해시 포함
- **답글이 이미 있는 코멘트**: 이미 답글이 달린 코멘트는 건너뜀

## GitHub API 참고

### 리뷰 코멘트 조회

```
GET /repos/{owner}/{repo}/pulls/{pull_number}/comments
```

주요 필드:
- `id`: 코멘트 고유 ID (답글 시 `in_reply_to`에 사용)
- `path`: 대상 파일 경로
- `line` / `original_line`: 코멘트가 달린 라인 번호
- `body`: 코멘트 본문
- `in_reply_to_id`: 답글 대상 코멘트 ID (null이면 원본 코멘트)

### 리뷰 코멘트에 답글

```
POST /repos/{owner}/{repo}/pulls/{pull_number}/comments
```

필수 파라미터:
- `body` (string): 답글 내용 → `-f body="..."`
- `in_reply_to` (integer): 원본 코멘트 ID → `-F in_reply_to=<id>` (`-F`로 숫자 전달)
