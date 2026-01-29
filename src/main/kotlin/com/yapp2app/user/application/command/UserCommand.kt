package com.yapp2app.user.application.command

/**
 * fileName       : UserCommand
 * author         : koo
 * date           : 2026. 1. 30. 오전 3:26
 * description    :
 */
data class GetUserCommand(val userId: Long)

data class UpdateUserCommand(val userId: Long, val mediaId: Long?, val name: String?)
