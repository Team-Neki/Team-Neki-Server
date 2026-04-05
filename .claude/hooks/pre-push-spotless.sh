#!/bin/bash
# Pre-push hook: spotless 포맷 검사 + 테스트 실행
# Claude Code PreToolUse hook으로 git push 전 자동 실행됨

INPUT=$(cat)
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command // ""')

if ! echo "$COMMAND" | grep -q "git push"; then
    exit 0
fi

cd "$CLAUDE_PROJECT_DIR" || exit 1

if ! ./gradlew spotlessCheck --quiet 2>&1; then
    echo "❌ Spotless 포맷 검사 실패. './gradlew spotlessApply' 실행 후 변경사항을 커밋하고 다시 push하세요." >&2
    exit 2
fi

if ! ./gradlew test --quiet 2>&1; then
    echo "❌ 테스트 실패. 테스트를 수정하고 다시 push하세요." >&2
    exit 2
fi