package com.neki.domain.support.external

import com.neki.domain.support.dto.ClientLogCommand

interface ClientLogSender {
    /** 한 건이라도 전달에 실패하면 예외를 던진다. 클라이언트가 배치 전체를 재전송하므로 중복이 생길 수 있다 (at-least-once). */
    fun send(command: ClientLogCommand.Collect)
}
