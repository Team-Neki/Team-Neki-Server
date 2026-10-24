package com.neki.domain.user.service

import com.neki.domain.user.models.AdminIdentity

interface AdminAccountAccess {
    fun findOrCreate(identity: AdminIdentity): Long

    fun isAdmin(userId: Long, identityKey: String): Boolean
}
