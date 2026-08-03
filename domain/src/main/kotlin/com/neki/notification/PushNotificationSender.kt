package com.neki.notification

interface PushNotificationSender {
    /**
     * 단일 디바이스 토큰으로 푸시 알림을 발송하고 메시지 ID 를 반환한다.
     *
     * @param link 알림 탭 시 앱이 이동할 딥링크 (예: neki://archive/123). null 이면 미포함.
     */
    fun send(token: String, title: String, body: String, link: String?): String
}
