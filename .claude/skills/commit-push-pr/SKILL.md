---
name: commit-push-pr
description: Commit, push, and open a PR following project conventions. Use when creating pull requests.
---

# Commit, Push & PR

## Context

- Current git status: !`git status`
- Current git diff (staged and unstaged changes): !`git diff HEAD`
- Current branch: !`git branch --show-current`
- PR template: !`cat .github/PULL_REQUEST_TEMPLATE.md`

## Conventions

### PR Title

Format: `{type}/#{issue_number} 설명`

Branch 이름에서 type과 issue number를 추출하고, 변경 사항을 요약하여 제목을 작성한다.

Examples:
- Branch `feat/#178` → `feat/#178 포즈 조회수 기능 추가`
- Branch `fix/#183` → `fix/#183 E2E 테스트 중간 테이블 기준으로 수정`
- Branch `docs/#186` → `docs/#186 PR template 생성`

### Base Branch (PR Chaining)

1. 현재 branch의 upstream tracking branch를 확인한다: `git rev-parse --abbrev-ref @{upstream}`
2. Tracking branch의 remote prefix(`origin/`)를 제거한 이름을 base branch로 사용한다.
3. Tracking branch가 없으면 `main`을 base로 사용한다.

### Issue 연결

Branch 이름에서 issue 번호를 추출한다.

- **Base가 `main`이 아닌 경우** (staging 등): `#{number}`로 링크만 남긴다.
- **Base가 `main`인 경우**: `closes #{number}`로 작성하여 merge 시 issue가 자동 종료되도록 한다.

### Assignee

항상 `--assignee @me`로 PR 작성자를 할당한다.

### Label

Branch prefix에 따라 label을 지정한다.

| Prefix   | Label           |
|----------|-----------------|
| `feat/`  | `enhancement`   |
| `fix/`   | `bug`           |
| `docs/`  | `documentation` |
| `chore/` | (none)          |

### Sub-issue Progress Table

현재 issue가 sub-issue인 경우, parent issue의 진행 상황을 table로 PR body에 포함한다.

확인 방법:
```bash
gh api graphql -f query='
  { repository(owner:"{owner}", name:"{repo}") {
      issue(number:{current_issue}) {
        trackedInIssues(first:1) { nodes { number title
          subIssues(first:20) { nodes { number title state } }
        } }
      }
  } }'
```

- `trackedInIssues`가 있으면 parent의 `subIssues`를 조회하여 table을 생성한다.
- 현재 issue는 `🔄 Current`로 표시한다.
- `CLOSED` 상태는 `✅ Done`, `OPEN` 상태는 `⬚ Open`으로 표시한다.
- Sub-issue가 없으면 Sub-issue Progress 섹션을 제거한다.

## Task

1. Stage all changes and create a single commit with an appropriate message.
2. Push the branch to origin.
3. `.github/PULL_REQUEST_TEMPLATE.md`를 사용하여 PR body를 생성한다. base branch와의 diff(`git diff {base}...HEAD`)를 기준으로 **현재 branch의 변경 사항만** 반영한다.
4. Sub-issue인 경우 GraphQL API로 progress table을 생성한다.
5. Create a PR using `gh pr create` with:
   - `--title`: convention에 맞는 제목
   - `--base`: PR Chaining 규칙에 따른 base branch
   - `--label`: branch prefix에 해당하는 label (매핑이 있는 경우만)
   - `--assignee @me`
   - `--body`: 템플릿을 채운 내용 (HEREDOC 사용)
6. You MUST do all of the above in a single message. Do not use any other tools or do anything else.