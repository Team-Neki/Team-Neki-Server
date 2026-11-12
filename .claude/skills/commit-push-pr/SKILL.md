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
- PR convention: !`cat docs/conventions/pull-request.md`

## Conventions

PR 제목·본문은 `docs/conventions/pull-request.md` 를 따른다. 아래는 `gh pr create` 옵션 규칙만 다룬다.

### Base Branch (PR Chaining)

push 하기 전에 정한다. `git push -u` 가 upstream 을 자기 자신으로 바꾸기 때문이다.

1. 현재 branch의 upstream tracking branch를 확인한다: `git rev-parse --abbrev-ref @{upstream}`
2. Tracking branch의 remote prefix(`origin/`)를 제거한 이름을 base branch로 사용한다.
3. Tracking branch가 없거나 자기 자신을 가리키면 `main`을 base로 사용한다.

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

## Task

1. Base Branch 규칙으로 base branch를 정한다 (push 전).
2. Stage all changes and create a single commit with an appropriate message.
3. Push the branch to origin (`git push -u origin <branch>`).
4. `docs/conventions/pull-request.md` 의 "PR 작성 프롬프트" 기준으로 제목과 본문을 쓴다. 1단계에서 정한 base와의 diff(`git diff {base}...HEAD`)를 기준으로 **현재 branch의 변경 사항만** 반영한다.
5. Create a PR using `gh pr create` with:
   - `--title`: convention에 맞는 제목
   - `--base`: 1단계에서 정한 base branch
   - `--label`: branch prefix에 해당하는 label (매핑이 있는 경우만)
   - `--assignee @me`
   - `--body`: 작성한 본문 (HEREDOC 사용)
6. 위 단계 외의 작업은 하지 않는다.
