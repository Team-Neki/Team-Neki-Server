package com.neki.user.application.usecase

import com.neki.common.exception.BusinessException
import com.neki.testfixture.FakeTransactionRunner
import com.neki.testfixture.aUser
import com.neki.user.application.command.RegisterOauthUserCommand
import com.neki.user.application.contract.OauthInfoPayload
import com.neki.user.application.port.AppleUserTransferRepositoryPort
import com.neki.user.application.port.AuthTokenProviderPort
import com.neki.user.application.port.NicknameGeneratorPort
import com.neki.user.application.port.OidcTokenValidatorPort
import com.neki.user.application.port.UserEventPublisherPort
import com.neki.user.application.port.UserRepositoryPort
import com.neki.user.domain.entity.AppleUserTransfer
import com.neki.user.domain.entity.User
import com.neki.user.domain.enums.Platform
import com.neki.user.domain.enums.ProviderType
import com.neki.user.domain.enums.RoleType
import com.neki.user.infra.security.config.OauthProperties
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient

class OauthLoginUseCaseTest {

    lateinit var oauthProperties: OauthProperties
    lateinit var oidcTokenValidatorPort: OidcTokenValidatorPort
    lateinit var restClient: RestClient
    lateinit var tokenProviderPort: AuthTokenProviderPort
    lateinit var userRepositoryPort: UserRepositoryPort
    lateinit var appleUserTransferRepositoryPort: AppleUserTransferRepositoryPort
    lateinit var nicknameGenerator: NicknameGeneratorPort
    lateinit var userEventPublisher: UserEventPublisherPort
    lateinit var transactionRunner: FakeTransactionRunner
    lateinit var useCase: OauthLoginUseCase

    @BeforeEach
    fun setUp() {
        oauthProperties = OauthProperties()
        oidcTokenValidatorPort = mockk()
        restClient = mockk()
        tokenProviderPort = mockk()
        userRepositoryPort = mockk()
        appleUserTransferRepositoryPort = mockk()
        nicknameGenerator = mockk()
        userEventPublisher = mockk()
        transactionRunner = FakeTransactionRunner()

        useCase = OauthLoginUseCase(
            oauthProperties = oauthProperties,
            oidcTokenValidatorPort = oidcTokenValidatorPort,
            restClient = restClient,
            tokenProviderPort = tokenProviderPort,
            userRepositoryPort = userRepositoryPort,
            appleUserTransferRepositoryPort = appleUserTransferRepositoryPort,
            nicknameGenerator = nicknameGenerator,
            userEventPublisher = userEventPublisher,
            transactionRunner = transactionRunner,
        )
    }

    @Test
    @DisplayName("기존 유저 로그인 - ID token 검증 후 유저 조회 및 토큰 생성")
    fun `기존 유저 로그인 - ID token 검증 후 유저 조회 및 토큰 생성`() {
        // Given
        val idToken = "valid-id-token"
        val existingUser =
            aUser(id = 1L, name = "기존유저", roles = RoleType.USER.role, providerType = ProviderType.KAKAO)
        val oauthInfoPayload = OauthInfoPayload(
            providerType = ProviderType.KAKAO,
            oid = "kakao-oid-123",
            email = "existing@example.com",
            name = "기존유저",
            imageUrl = null,
        )
        val command = RegisterOauthUserCommand(
            idToken = idToken,
            providerType = ProviderType.KAKAO,
            platform = Platform.IOS,
        )

        every {
            oidcTokenValidatorPort.validateIdToken(idToken, ProviderType.KAKAO, Platform.IOS)
        } returns oauthInfoPayload
        every {
            userRepositoryPort.findByOid(oid = "kakao-oid-123", provider = ProviderType.KAKAO)
        } returns existingUser
        every {
            tokenProviderPort.createAccessToken(
                id = "1",
                roles = listOf(RoleType.USER.role),
                name = "기존유저",
                providerType = ProviderType.KAKAO,
            )
        } returns "access-token"
        every {
            tokenProviderPort.createRefreshToken(
                id = "1",
                roles = listOf(RoleType.USER.role),
                name = "기존유저",
                providerType = ProviderType.KAKAO,
            )
        } returns "refresh-token"

        // When
        val result = useCase.execute(command)

        // Then
        result.accessToken shouldBe "access-token"
        result.refreshToken shouldBe "refresh-token"
        verify(exactly = 0) { nicknameGenerator.generateUniqueNickname() }
        verify(exactly = 0) { userRepositoryPort.save(any()) }
    }

    @Test
    @DisplayName("신규 유저 가입 - 미존재 유저 감지 후 닉네임 생성 및 저장 후 토큰 생성")
    fun `신규 유저 가입 - 미존재 유저 감지 후 닉네임 생성 및 저장 후 토큰 생성`() {
        // Given
        val idToken = "valid-id-token"
        val oauthInfoPayload = OauthInfoPayload(
            providerType = ProviderType.KAKAO,
            oid = "kakao-oid-new",
            email = "new@example.com",
            name = null,
            imageUrl = null,
        )
        val command = RegisterOauthUserCommand(
            idToken = idToken,
            providerType = ProviderType.KAKAO,
            platform = Platform.IOS,
        )
        val savedUser =
            aUser(id = 2L, name = "랜덤닉네임", roles = RoleType.USER.role, providerType = ProviderType.KAKAO)

        every {
            oidcTokenValidatorPort.validateIdToken(idToken, ProviderType.KAKAO, Platform.IOS)
        } returns oauthInfoPayload
        every {
            userRepositoryPort.findByOid(oid = "kakao-oid-new", provider = ProviderType.KAKAO)
        } returns null
        every { nicknameGenerator.generateUniqueNickname() } returns "랜덤닉네임"
        every { userRepositoryPort.save(any<User>()) } returns savedUser
        every { userRepositoryPort.countByOidIsNotNull() } returns 1L
        every { userEventPublisher.publish(any()) } returns Unit
        every {
            tokenProviderPort.createAccessToken(
                id = "2",
                roles = listOf(RoleType.USER.role),
                name = "랜덤닉네임",
                providerType = ProviderType.KAKAO,
            )
        } returns "new-access-token"
        every {
            tokenProviderPort.createRefreshToken(
                id = "2",
                roles = listOf(RoleType.USER.role),
                name = "랜덤닉네임",
                providerType = ProviderType.KAKAO,
            )
        } returns "new-refresh-token"

        // When
        val result = useCase.execute(command)

        // Then
        result.accessToken shouldBe "new-access-token"
        result.refreshToken shouldBe "new-refresh-token"
        verify(exactly = 1) { nicknameGenerator.generateUniqueNickname() }
        verify(exactly = 1) { userRepositoryPort.save(any()) }
    }

    @Test
    @DisplayName("Apple 로그인 방어 - new sub 미존재 + 매핑 new_sub 존재 시 기존 유저 oid 갱신 후 로그인")
    fun `Apple new_sub 매핑으로 기존 유저를 찾아 oid 를 갱신한다`() {
        // Given
        val idToken = "apple-id-token"
        val newSub = "apple-new-B-sub"
        val existingUser = aUser(
            id = 10L,
            name = "기존애플유저",
            roles = RoleType.USER.role,
            providerType = ProviderType.APPLE,
            oid = "apple-old-A-sub",
        )
        val payload = OauthInfoPayload(
            providerType = ProviderType.APPLE,
            oid = newSub,
            email = "a@privaterelay.appleid.com",
            name = "a",
            imageUrl = null,
        )
        val command = RegisterOauthUserCommand(idToken, ProviderType.APPLE, Platform.IOS)
        val mapping = AppleUserTransfer(
            userId = 10L,
            oldSub = "apple-old-A-sub",
            transferSub = "apple-transfer-sub",
            newSub = newSub,
        )

        every { oidcTokenValidatorPort.validateIdToken(idToken, ProviderType.APPLE, Platform.IOS) } returns payload
        every { userRepositoryPort.findByOid(oid = newSub, provider = ProviderType.APPLE) } returns null
        every { appleUserTransferRepositoryPort.findByNewSub(newSub) } returns mapping
        every { userRepositoryPort.findById(10L) } returns existingUser
        every { userRepositoryPort.save(existingUser) } returns existingUser
        every {
            tokenProviderPort.createAccessToken(
                id = "10",
                roles = listOf(RoleType.USER.role),
                name = "기존애플유저",
                providerType = ProviderType.APPLE,
            )
        } returns "at"
        every {
            tokenProviderPort.createRefreshToken(
                id = "10",
                roles = listOf(RoleType.USER.role),
                name = "기존애플유저",
                providerType = ProviderType.APPLE,
            )
        } returns "rt"

        // When
        val result = useCase.execute(command)

        // Then
        result.accessToken shouldBe "at"
        existingUser.oid shouldBe newSub
        verify(exactly = 1) { userRepositoryPort.save(existingUser) }
        verify(exactly = 0) { nicknameGenerator.generateUniqueNickname() }
    }

    @Test
    @DisplayName("Apple 로그인 - new sub/매핑 모두 미매칭 시 신규 가입")
    fun `Apple new_sub 매핑이 없으면 신규 가입한다`() {
        // Given
        val idToken = "apple-id-token"
        val payload = OauthInfoPayload(
            providerType = ProviderType.APPLE,
            oid = "apple-brand-new",
            email = "new@privaterelay.appleid.com",
            name = "new",
            imageUrl = null,
        )
        val command = RegisterOauthUserCommand(idToken, ProviderType.APPLE, Platform.IOS)
        val savedUser = aUser(id = 20L, name = "랜덤", roles = RoleType.USER.role, providerType = ProviderType.APPLE)

        every { oidcTokenValidatorPort.validateIdToken(idToken, ProviderType.APPLE, Platform.IOS) } returns payload
        every { userRepositoryPort.findByOid(oid = "apple-brand-new", provider = ProviderType.APPLE) } returns null
        every { appleUserTransferRepositoryPort.findByNewSub("apple-brand-new") } returns null
        every { nicknameGenerator.generateUniqueNickname() } returns "랜덤"
        every { userRepositoryPort.save(any<User>()) } returns savedUser
        every { userRepositoryPort.countByOidIsNotNull() } returns 1L
        every { userEventPublisher.publish(any()) } returns Unit
        every {
            tokenProviderPort.createAccessToken(
                id = "20",
                roles = listOf(RoleType.USER.role),
                name = "랜덤",
                providerType = ProviderType.APPLE,
            )
        } returns "at"
        every {
            tokenProviderPort.createRefreshToken(
                id = "20",
                roles = listOf(RoleType.USER.role),
                name = "랜덤",
                providerType = ProviderType.APPLE,
            )
        } returns "rt"

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { nicknameGenerator.generateUniqueNickname() }
        verify(exactly = 1) { userRepositoryPort.save(any()) }
    }

    @Test
    @DisplayName("ID token 검증 실패 - oidcTokenValidatorPort 예외 전파 확인")
    fun `ID token 검증 실패 - oidcTokenValidatorPort 예외 전파 확인`() {
        // Given
        val idToken = "invalid-id-token"
        val command = RegisterOauthUserCommand(
            idToken = idToken,
            providerType = ProviderType.KAKAO,
            platform = Platform.IOS,
        )

        every {
            oidcTokenValidatorPort.validateIdToken(idToken, ProviderType.KAKAO, Platform.IOS)
        } throws BusinessException(com.neki.common.code.ResultCode.INVALID_TOKEN_ERROR)

        // When & Then
        shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        verify(exactly = 0) { userRepositoryPort.findByOid(any(), any()) }
    }

    @Test
    @DisplayName("유저 저장 후 토큰 생성 실패 - createAccessToken 예외 전파 확인")
    fun `유저 저장 후 토큰 생성 실패 - createAccessToken 예외 전파 확인`() {
        // Given
        val idToken = "valid-id-token"
        val oauthInfoPayload = OauthInfoPayload(
            providerType = ProviderType.KAKAO,
            oid = "kakao-oid-new",
            email = "new@example.com",
            name = null,
            imageUrl = null,
        )
        val command = RegisterOauthUserCommand(
            idToken = idToken,
            providerType = ProviderType.KAKAO,
            platform = Platform.IOS,
        )
        val savedUser =
            aUser(id = 3L, name = "닉네임", roles = RoleType.USER.role, providerType = ProviderType.KAKAO)

        every {
            oidcTokenValidatorPort.validateIdToken(idToken, ProviderType.KAKAO, Platform.IOS)
        } returns oauthInfoPayload
        every {
            userRepositoryPort.findByOid(oid = "kakao-oid-new", provider = ProviderType.KAKAO)
        } returns null
        every { nicknameGenerator.generateUniqueNickname() } returns "닉네임"
        every { userRepositoryPort.save(any<User>()) } returns savedUser
        every { userRepositoryPort.countByOidIsNotNull() } returns 1L
        every { userEventPublisher.publish(any()) } returns Unit
        every {
            tokenProviderPort.createAccessToken(
                id = "3",
                roles = listOf(RoleType.USER.role),
                name = "닉네임",
                providerType = ProviderType.KAKAO,
            )
        } throws RuntimeException("토큰 생성 실패")

        // When & Then
        shouldThrow<RuntimeException> {
            useCase.execute(command)
        }
    }

    @Test
    @DisplayName("roles 문자열 split - 'USER,ADMIN'을 split(',')하면 2개 원소 리스트 반환")
    fun `roles 문자열 split - 'USER,ADMIN'을 split(',')하면 2개 원소 리스트 반환`() {
        // Given
        val idToken = "valid-id-token"
        val userWithMultipleRoles =
            aUser(id = 4L, name = "관리자", roles = "USER,ADMIN", providerType = ProviderType.KAKAO)
        val oauthInfoPayload = OauthInfoPayload(
            providerType = ProviderType.KAKAO,
            oid = "kakao-oid-admin",
            email = "admin@example.com",
            name = "관리자",
            imageUrl = null,
        )
        val command = RegisterOauthUserCommand(
            idToken = idToken,
            providerType = ProviderType.KAKAO,
            platform = Platform.IOS,
        )

        every {
            oidcTokenValidatorPort.validateIdToken(idToken, ProviderType.KAKAO, Platform.IOS)
        } returns oauthInfoPayload
        every {
            userRepositoryPort.findByOid(oid = "kakao-oid-admin", provider = ProviderType.KAKAO)
        } returns userWithMultipleRoles

        val capturedRoles = mutableListOf<Collection<String>>()
        every {
            tokenProviderPort.createAccessToken(
                id = "4",
                roles = capture(capturedRoles),
                name = "관리자",
                providerType = ProviderType.KAKAO,
            )
        } returns "access-token-admin"
        every {
            tokenProviderPort.createRefreshToken(
                id = "4",
                roles = any(),
                name = "관리자",
                providerType = ProviderType.KAKAO,
            )
        } returns "refresh-token-admin"

        // When
        useCase.execute(command)

        // Then
        capturedRoles.size shouldBe 1
        val roles = capturedRoles[0].toList()
        roles.size shouldBe 2
        roles shouldBe listOf("USER", "ADMIN")
    }
}
