package com.yapp2app.user.application.command

/**
 * fileName       : UpdateUserCommand
 * author         : koo
 * date           : 2026. 1. 28. 오후 3:47
 * description    :
 */
data class UpdateUserCommand(val userId: Long, val mediaId: Long?, val name: String?)
