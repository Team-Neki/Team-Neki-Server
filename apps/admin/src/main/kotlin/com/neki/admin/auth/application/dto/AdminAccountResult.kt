package com.neki.admin.auth.application.dto

data class AdminAccountResult(val userId: Long, val email: String?, val name: String?, val admin: Boolean)
