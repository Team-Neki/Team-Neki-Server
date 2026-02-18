package com.neki.user.api.controller

import com.neki.common.api.document.RequiresSecurity
import com.neki.common.api.dto.BaseResponse
import com.neki.user.api.converter.UserCommandConverter
import com.neki.user.api.converter.UserResultConverter
import com.neki.user.api.dto.GetUserResponse
import com.neki.user.api.dto.UpdateUserProfileImageRequest
import com.neki.user.api.dto.UpdateUserRequest
import com.neki.user.application.usecase.DeleteMeUseCase
import com.neki.user.application.usecase.GetUserInfoUseCase
import com.neki.user.application.usecase.UpdateMeUseCase
import com.neki.user.application.usecase.UpdateUserProfileImageUseCase
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
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

    private val commandConverter: UserCommandConverter,
    private val resultConverter: UserResultConverter,
) {

    @Operation(
        summary = "내 정보 조회",
        description = """
        AccessToken 만료 시 HttpStatus 401

        """,
    )
    @GetMapping("/info")
    fun info(@AuthenticationPrincipal(expression = "id") userId: Long): BaseResponse<GetUserResponse> {
        val command = commandConverter.toGetUserCommand(userId)

        val result = getUserInfoUseCase.execute(command)

        val response = resultConverter.toGetUserResponse(result)

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
        @Valid @RequestBody request: UpdateUserRequest,
    ): BaseResponse<Any> {
        val command = commandConverter.toUpdateUserCommand(userId, request)

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
        @Valid @RequestBody request: UpdateUserProfileImageRequest,
    ): BaseResponse<Any> {
        val command = commandConverter.toUpdateUserProfileImageCommand(userId, request)

        updateProfileUseCase.execute(command)

        return BaseResponse()
    }

    @Operation(
        summary = "회원탈퇴",
        description = "회원탈퇴를 진행합니다.",
    )
    @DeleteMapping("/me")
    fun deleteMe(@AuthenticationPrincipal(expression = "id") userId: Long): BaseResponse<Any> {
        val command = commandConverter.toDeleteUserCommand(userId)

        deleteMeUseCase.execute(command)

        return BaseResponse()
    }
}
