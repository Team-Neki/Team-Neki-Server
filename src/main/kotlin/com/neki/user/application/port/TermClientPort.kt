package com.neki.user.application.port

interface TermClientPort {

    fun hasAgreedToLatestTerms(userId: Long): Boolean
}
