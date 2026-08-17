package com.neki.domain.user.models

enum class RoleType(val role: String, val roleName: String) {
    USER("ROLE_USER", "일반"),
    ADMIN("ROLE_ADMIN", "관리자"),
}
