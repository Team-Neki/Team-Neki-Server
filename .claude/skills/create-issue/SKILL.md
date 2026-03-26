---
name: create-issue
description: Create a GitHub issue following project conventions. Use when creating issues.
---

# Create Issue

## Context

- Available issue templates: !`ls .github/ISSUE_TEMPLATE/`
- Feature template: !`cat .github/ISSUE_TEMPLATE/feature.yml`
- Bug template: !`cat .github/ISSUE_TEMPLATE/bug.yml`
- Chore template: !`cat .github/ISSUE_TEMPLATE/chore.yml`
- Docs template: !`cat .github/ISSUE_TEMPLATE/docs.yml`

## Conventions

### Issue Title

Format: `{type}: 설명`

| Type | 용도 | Label |
|------|------|-------|
| `feat` | 새로운 기능 | `enhancement` |
| `fix` | 버그 수정 | `bug` |
| `docs` | 문서 작업 | `documentation` |
| `chore` | 리팩토링, 설정 등 | (none) |

Examples:
- `feat: 사진 복제 API`
- `fix: 읽기 전환 및 folder_id 컬럼 제거`
- `chore: GitOps 배포 파이프라인 적용`

### Issue Body

`.github/ISSUE_TEMPLATE/` 의 해당 type template을 읽고, template의 필드 구조에 맞춰 body를 작성한다.

### Sub-issue

Parent issue가 지정된 경우, issue 생성 후 parent에 sub-issue로 연결한다:
```bash
gh api graphql -f query='mutation { addSubIssue(input: {issueId: "{parent_node_id}", subIssueId: "{new_issue_node_id}"}) { issue { id } } }'
```

### Assignee

항상 `--assignee @me`로 본인에게 할당한다.

## Task

1. 사용자의 요청에서 issue type을 판단한다.
2. 해당 type의 template 파일(`.github/ISSUE_TEMPLATE/{type}.yml`)을 읽는다.
3. Template의 필드 구조에 맞춰 issue body를 작성한다.
4. `gh issue create`로 issue를 생성한다:
   - `--title`: convention에 맞는 제목
   - `--label`: type에 해당하는 label
   - `--assignee @me`
   - `--body`: template 기반 내용
5. Parent issue가 있으면 sub-issue로 연결한다.
6. 생성된 issue URL을 출력한다.