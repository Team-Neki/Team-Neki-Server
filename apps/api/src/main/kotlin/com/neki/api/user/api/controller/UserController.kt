package com.neki.api.user.api.controller

import com.neki.api.common.api.document.RequiresSecurity
import com.neki.api.user.api.dto.UserConverter
import com.neki.api.user.api.dto.UserRequest
import com.neki.api.user.api.dto.UserResponse
import com.neki.api.user.application.DeleteMeUseCase
import com.neki.api.user.application.GetUserInfoUseCase
import com.neki.api.user.application.LogoutUseCase
import com.neki.api.user.application.UpdateMeUseCase
import com.neki.api.user.application.UpdateUserProfileImageUseCase
import com.neki.api.user.application.dto.UserResult
import com.neki.core.api.dto.BaseResponse
import com.neki.domain.user.dto.UserCommand
import com.neki.domain.user.dto.UserQuery
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : UserController
 * author         : koo
 * date           : 2025. 12. 28. 오후 7:39
 * description    :
 */
@RequiresSecurity
@RestController
@RequestMapping("/api/users")
class UserController(
    private val updateMeUseCase: UpdateMeUseCase,
    private val updateProfileUseCase: UpdateUserProfileImageUseCase,
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val deleteMeUseCase: DeleteMeUseCase,
    private val logoutUseCase: LogoutUseCase,

    private val requestConverter: UserConverter.RequestConverter,
    private val responseConverter: UserConverter.ResponseConverter,
) {

    @Operation(
        summary = "내 정보 조회",
        description = """
        AccessToken 만료 시 HttpStatus 401

        """,
    )
    @GetMapping("/info")
    fun info(@AuthenticationPrincipal(expression = "id") userId: Long): BaseResponse<UserResponse.GetUser> {
        val query: UserQuery.GetUser = requestConverter.toGetUserQuery(userId)

        val result: UserResult.GetUser = getUserInfoUseCase.execute(query)

        val response: UserResponse.GetUser = responseConverter.toGetUserResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "내 정보 갱신",
        description = """내 정보를 갱신합니다. 이미지 변경이 있는 경우
            1. /media/upload로 upload ticket 발급
            2. object storage에 이미지 업로드
            3. /api/users/me
            순서로 진행합니다.
            프로필 사진을 기본 이미지로 변경할 때는 mediaId에 null을 전달합니다.""",
    )
    @PatchMapping("/me")
    fun updateMe(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: UserRequest.UpdateUser,
    ): BaseResponse<Any> {
        val command: UserCommand.UpdateUserInfo = requestConverter.toUpdateUserCommand(userId, request)

        updateMeUseCase.execute(command)

        return BaseResponse()
    }

    @Operation(
        summary = "프로필 이미지 변경",
        description = "프로필 이미지를 변경합니다. 이미지 변경이 있는 경우\n" +
            "1. /media/upload로 upload ticket 발급\n" +
            "2. object storage에 이미지 업로드\n" +
            "3. /api/users/me/profile-image\n" +
            "순서로 진행합니다.\n" +
            "프로필 사진을 기본 이미지로 변경할 때는 mediaId에 null을 전달합니다.",
    )
    @PatchMapping("/me/profile-image")
    fun updateProfileImage(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: UserRequest.UpdateUserProfileImage,
    ): BaseResponse<Any> {
        val command: UserCommand.UpdateUserProfileImage = requestConverter.toUpdateUserProfileImageCommand(
            userId,
            request,
        )

        updateProfileUseCase.execute(command)

        return BaseResponse()
    }

    @Operation(
        summary = "회원탈퇴",
        description = "회원탈퇴를 진행합니다.",
    )
    @DeleteMapping("/me")
    fun deleteMe(@AuthenticationPrincipal(expression = "id") userId: Long): BaseResponse<Any> {
        val command: UserCommand.DeleteUser = requestConverter.toDeleteUserCommand(userId)

        deleteMeUseCase.execute(command)

        return BaseResponse()
    }

    @Operation(
        summary = "로그아웃",
        description = "로그아웃을 진행합니다. 사용자의 FCM 토큰을 삭제하여 더 이상 푸시 알림이 전송되지 않도록 합니다.",
    )
    @PostMapping("/logout")
    fun logout(@AuthenticationPrincipal(expression = "id") userId: Long): BaseResponse<Any> {
        val command: UserCommand.Logout = requestConverter.toLogoutCommand(userId)

        logoutUseCase.execute(command)

        return BaseResponse()
    }
}
