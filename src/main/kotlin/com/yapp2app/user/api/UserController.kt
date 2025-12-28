package com.yapp2app.user.api

import com.yapp2app.common.api.dto.BaseResponse
import com.yapp2app.user.application.command.RegisterCommand
import com.yapp2app.user.application.usecase.RegisterUseCase
import com.yapp2app.user.domain.enums.ProviderType
import io.swagger.v3.oas.annotations.Hidden
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
@RestController
@RequestMapping("/api/users")
class UserController(private val registerUseCase: RegisterUseCase) {

    @Hidden
    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): BaseResponse<Any> {
        registerUseCase.execute(
            RegisterCommand(
                request.email,
                request.name,
                request.password,
                ProviderType.LOCAL,
            ),
        )

        return BaseResponse()
    }
}
