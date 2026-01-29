package com.yapp2app.user.api.controller

import com.yapp2app.auth.infra.security.token.UserPrincipal
import com.yapp2app.common.api.document.RequiresSecurity
import com.yapp2app.common.api.dto.BaseResponse
import com.yapp2app.user.api.converter.UserCommandConverter
import com.yapp2app.user.api.dto.GetUserInfoResponse
import com.yapp2app.user.api.dto.UpdateUserRequest
import com.yapp2app.user.application.usecase.UpdateUserUseCase
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
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
    private val commandConverter: UserCommandConverter,
    private val updateUserUseCase: UpdateUserUseCase,
) {

    @Operation(
        summary = "내 정보 조회",
        description = """
        AccessToken 만료 시 HttpStatus 401

        """,
    )
    @GetMapping("/info")
    fun info(@AuthenticationPrincipal userPrincipal: UserPrincipal): BaseResponse<GetUserInfoResponse> = BaseResponse(
        data = GetUserInfoResponse(
            name = userPrincipal.name!!,
            providerType = userPrincipal.providerType,
        ),
    )

    @Operation(
        summary = "내 정보 갱신",
        description = "내 정보를 갱신합니다",
    )
    @PatchMapping("/me")
    fun updateMe(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: UpdateUserRequest,
    ): BaseResponse<Any> {
        val command = commandConverter.toUpdateUserCommand(userId, request)

        updateUserUseCase.execute(command)

        return BaseResponse()
    }
}
