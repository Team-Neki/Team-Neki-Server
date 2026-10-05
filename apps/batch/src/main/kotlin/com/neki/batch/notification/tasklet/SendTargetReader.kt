package com.neki.batch.notification.step

import com.neki.domain.notification.models.Notification
import com.neki.domain.notification.models.SendTarget

/**
 * 잡별 대상 조회. 모든 구현은 NotificationRepository.findPushAgreedAfter 로 동의자 페이지를 받아
 * 자기 조건으로 거르고 변수를 채운다. 동의 필터는 그 한 곳이 단일 출처다
 */
fun interface SendTargetReader {
    fun readPage(afterUserId: Long, size: Int): SendTargetPage
}

/**
 * 한 페이지의 결과. nextCursor 는 거르기 전 동의자 페이지의 마지막 user_id 이고 null 이면 소진이다.
 * targets 가 비어도 nextCursor 가 있으면 소진이 아니다 (조건이 좁은 잡은 한 페이지가 통째로 걸러질 수 있다)
 */
data class SendTargetPage(val targets: List<SendTarget>, val nextCursor: Long?) {
    companion object {
        val EXHAUSTED = SendTargetPage(emptyList(), null)

        /** 동의자 페이지가 size 보다 작으면 마지막 페이지다 */
        fun cursorOf(page: List<Notification>, size: Int): Long? = if (page.size < size) null else page.last().userId
    }
}
