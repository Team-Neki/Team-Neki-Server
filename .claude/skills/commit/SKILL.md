---
name: commit
description: 프로젝트 컨벤션에 맞는 일관된 커밋 메시지를 생성하고 커밋을 수행합니다.
---

# Commit Skill

코드 작업이 완료된 후, 프로젝트 커밋 메시지 컨벤션에 맞는 커밋을 생성합니다.

---

## 사용법

### 사용 시점

코드 작업이 **완료된 후** 커밋을 생성할 때 사용합니다.

### 호출 방법

```
/commit
```

### 동작 흐름

1. `git status`로 변경 사항 확인
2. `git diff --staged`로 스테이징된 변경 내용 분석
3. `git log --oneline -10`으로 최근 커밋 스타일 참조
4. 아래 컨벤션에 맞는 커밋 메시지 생성
5. `git commit` 수행

---

## 커밋 메시지 컨벤션

### 형식

```
<type>: <한국어 설명>
```

### 허용 타입

| Type       | 설명        |
|------------|-----------|
| `feat`     | 새로운 기능 추가 |
| `fix`      | 버그 수정     |
| `refactor` | 리팩토링      |
| `test`     | 테스트 추가/수정 |
| `chore`    | 빌드, 설정 변경 |
| `docs`     | 문서 변경     |

### 작성 규칙

- 설명은 **한국어**로 작성
- 간결하게 **무엇을 했는지** 명확히 기술
- 메시지 끝에 Co-Authored-By 포함:
  ```
  Co-Authored-By: Claude <model> <noreply@anthropic.com>
  ```

### 예시

```
feat: 사진 이동 API 추가 (PATCH /api/folders/{sourceFolderId}/photos/move)

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
```

```
refactor: 폴더 소유권 확인을 단일 쿼리로 개선

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
```

---

## 규칙

- **staged 파일만 커밋**: `.env`, credentials 등 민감 파일은 절대 staging하지 않음
- **하나의 논리적 변경 단위**: 한 커밋에 하나의 목적만 담음
- **spotlessApply**: hook이 자동 실행하므로 skill에서 별도 실행 불필요
- **--amend 금지**: 항상 새로운 커밋을 생성 (hook 실패 시에도 새 커밋으로 재시도)