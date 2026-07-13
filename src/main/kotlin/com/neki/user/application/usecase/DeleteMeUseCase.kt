package com.neki.user.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.user.application.command.DeleteUserCommand
import com.neki.user.application.port.NotificationClientPort
import com.neki.user.application.port.TermClientPort
import com.neki.user.application.port.UserEventPublisherPort
import com.neki.user.application.port.UserRepositoryPort
import com.neki.user.domain.entity.User
import com.neki.user.event.UserWithdrawnEvent
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : DeleteMeUseCase
 * author         : koo
 * date           : 2026. 1. 30. 오후 5:49
 * description    : 회원탍퇴 usecase
 * - 회원 탈퇴 시, 사용자 정보는 삭제하지 않고 상태만 '탈퇴'로 변경
 */
@UseCase
class DeleteMeUseCase(
    private val userRepository: UserRepositoryPort,
    private val userEventPublisher: UserEventPublisherPort,
    private val termClient: TermClientPort,
    private val notificationClient: NotificationClientPort,
) {

    @Transactional
    fun execute(command: DeleteUserCommand) {
        val user: User = userRepository.findById(command.userId)
            ?: throw BusinessException(ResultCode.NOT_FOUND_USER)

        user.withdraw()
        termClient.revokeOptionalTerms(command.userId)
        notificationClient.deleteFcmToken(command.userId)

        val activeUserCount: Long = userRepository.countByOidIsNotNull()

        userEventPublisher.publish(
            UserWithdrawnEvent(userId = user.id!!, nickname = user.name!!, activeUserCount = activeUserCount),
        )
    }
}
