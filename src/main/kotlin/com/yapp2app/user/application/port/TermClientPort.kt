package com.yapp2app.user.application.port

interface TermClientPort {

    fun hasAgreedToLatestTerms(userId: Long): Boolean
}
