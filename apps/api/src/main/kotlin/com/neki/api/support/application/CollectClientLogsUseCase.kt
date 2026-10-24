package com.neki.api.support.application

import com.neki.core.annotation.UseCase
import com.neki.domain.support.dto.ClientLogCommand
import com.neki.domain.support.external.ClientLogSender

/**
 * fileName       : CollectClientLogsUseCase
 * author         : koo
 * date           : 2026. 10. 2.
 * description    : 클라이언트 로그를 Kinesis Firehose 로 전달
 */
@UseCase
class CollectClientLogsUseCase(private val clientLogSender: ClientLogSender) {

    fun execute(command: ClientLogCommand.Collect) {
        clientLogSender.send(command)
    }
}
