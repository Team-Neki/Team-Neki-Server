package com.neki.domain.support.infra.firehose.fake

import com.neki.domain.support.dto.ClientLogCommand
import com.neki.domain.support.external.ClientLogSender
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.CopyOnWriteArrayList

/**
 * fileName       : FakeClientLogSender
 * author         : koo
 * date           : 2026. 10. 2.
 * description    : test·local 용 ClientLogSender. 받은 command 를 메모리에 쌓기만 한다.
 */
@Profile("test | local")
@Component
class FakeClientLogSender : ClientLogSender {

    val sent = CopyOnWriteArrayList<ClientLogCommand.Collect>()

    override fun send(command: ClientLogCommand.Collect) {
        sent.add(command)
    }
}
