package com.neki.api.support.api.dto

import com.neki.domain.support.dto.ClientLogCommand
import org.springframework.stereotype.Component

/**
 * fileName       : ClientLogConverter
 * author         : koo
 * date           : 2026. 10. 2.
 * description    : ClientLog api layer converter
 */
object ClientLogConverter {
    @Component
    class RequestConverter {
        fun toCollectCommand(userId: Long, request: ClientLogRequest.Collect): ClientLogCommand.Collect =
            ClientLogCommand.Collect(
                userId = userId,
                platform = request.platform,
                appVersion = request.appVersion,
                logs = request.logs,
            )
    }
}
