package com.neki.domain.user.service

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.user.dto.UserCommand
import com.neki.domain.user.dto.UserQuery
import com.neki.domain.user.external.NicknameGenerator
import com.neki.domain.user.models.OauthRegistration
import com.neki.domain.user.models.OauthUserInfo
import com.neki.domain.user.models.RoleType
import com.neki.domain.user.models.User
import com.neki.domain.user.repository.UserRepository
import org.springframework.stereotype.Component

/**
 * fileName       : UserService
 * author         : koo
 * date           : 2026. 8. 3. 오전 1:57
 * description    : User 도메인 서비스
 */
@Component
class UserService(private val userRepositoryPort: UserRepository, private val nicknameGenerator: NicknameGenerator) {

    fun getUser(query: UserQuery.GetUser): User = userRepositoryPort.findById(query.userId)
        ?: throw BusinessException(ResultCode.NOT_FOUND_USER)

    /**
     * OAuth 사용자 조회, 미가입 상태면 신규 가입 처리
     */
    fun registerOauthUserIfAbsent(oauthUserInfo: OauthUserInfo): OauthRegistration {
        val existingUser: User? = userRepositoryPort.findByOid(
            oid = oauthUserInfo.oid,
            provider = oauthUserInfo.providerType,
        )

        if (existingUser != null) {
            return OauthRegistration(existingUser, isNew = false)
        }

        val nickname: String = nicknameGenerator.generateUniqueNickname()
        val newUser: User = userRepositoryPort.save(
            User(
                email = oauthUserInfo.email,
                oid = oauthUserInfo.oid,
                name = nickname,
                roles = RoleType.USER.role,
                providerType = oauthUserInfo.providerType,
                profileImageId = null,
            ),
        )
        return OauthRegistration(newUser, isNew = true)
    }

    fun updateUserInfo(command: UserCommand.UpdateUserInfo) {
        val user: User = userRepositoryPort.findById(command.userId)
            ?: throw BusinessException(ResultCode.NOT_FOUND_USER)

        user.updateName(command.name)
    }

    /**
     * 프로필 이미지 변경 (멱등: 동일 이미지면 no-op)
     *
     * @return 변경으로 대체된 이전 profileImageId (변경 없음/이전 이미지 없음이면 null)
     */
    fun updateProfileImage(command: UserCommand.UpdateUserProfileImage): Long? {
        val user: User = userRepositoryPort.findById(command.userId)
            ?: throw BusinessException(ResultCode.NOT_FOUND_USER)

        if (user.profileImageId == command.mediaId) return null

        val oldMediaId: Long? = user.profileImageId
        user.updateProfileImage(command.mediaId)
        return oldMediaId
    }

    fun withdrawUser(command: UserCommand.DeleteUser): User {
        val user: User = userRepositoryPort.findById(command.userId)
            ?: throw BusinessException(ResultCode.NOT_FOUND_USER)

        user.withdraw()

        return user
    }

    fun countActiveUsers(): Long = userRepositoryPort.countByOidIsNotNull()
}
