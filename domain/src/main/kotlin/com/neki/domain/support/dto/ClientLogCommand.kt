package com.neki.domain.support.dto

import com.neki.domain.support.models.Platform

/**
 * fileName       : ClientLogCommand
 * author         : koo
 * date           : 2026. 10. 2.
 * description    : 클라이언트 로그 수집 command
 */
object ClientLogCommand {
    /**
     * logs 의 각 원소는 클라이언트가 보낸 JSON 객체 그대로이며 Firehose 레코드 1건이 된다.
     * appVersion 도 원소 안에 있다. 배치 단위로 받으면 쌓아 둔 로그를 업데이트 뒤에 보낼 때 버전이 틀어진다.
     */
    data class Collect(val userId: Long, val platform: Platform, val logs: List<Map<String, Any?>>)
}
