package com.neki.domain.user.service

import com.neki.domain.user.dto.UserCommand

interface AdminAccountAccess {
    fun findOrCreate(command: UserCommand.RegisterAdminAccount): Long

    fun isAdmin(command: UserCommand.AdminAccountAccess): Boolean
}
