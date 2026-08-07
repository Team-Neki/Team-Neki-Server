package com.neki.user.application

import com.neki.common.annotation.UseCase
import com.neki.user.client.NotificationClient
import com.neki.user.client.TermClient
import com.neki.user.dto.UserCommand
import com.neki.user.external.UserEventPublisher
import com.neki.user.models.User
import com.neki.user.models.UserWithdrawnEvent
import com.neki.user.service.UserService
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : DeleteMeUseCase
 * author         : koo
 * date           : 2026. 1. 30. 오후 5:49
 * description    : 회원탈퇴 usecase
 * - 회원 탈퇴 시, 사용자 정보는 삭제하지 않고 상태만 '탈퇴'로 변경
 */
@UseCase
class DeleteMeUseCase(
    private val userEventPublisher: UserEventPublisher,
    private val termClient: TermClient,
    private val notificationClient: NotificationClient,
    private val userService: UserService,
) {

    @Transactional
    fun execute(command: UserCommand.DeleteUser) {
        val user: User = userService.withdrawUser(command)

        termClient.revokeOptionalTerms(user.id!!)

        notificationClient.deleteFcmToken(user.id!!)

        val activeUserCount: Long = userService.countActiveUsers()

        userEventPublisher.publish(
            UserWithdrawnEvent(userId = user.id!!, nickname = user.name!!, activeUserCount = activeUserCount),
        )
    }
}
