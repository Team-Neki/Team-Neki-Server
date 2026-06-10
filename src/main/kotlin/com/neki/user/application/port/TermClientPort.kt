package com.neki.user.application.port

interface TermClientPort {

    fun hasAgreedToAllRequired(userId: Long): Boolean

    fun hasAgreedToMarketing(userId: Long): Boolean

    fun revokeOptionalTerms(userId: Long)
}
