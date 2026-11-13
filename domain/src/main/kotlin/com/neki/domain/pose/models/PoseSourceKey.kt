package com.neki.domain.pose.models

/**
 * fileName       : PoseSourceKey
 * author         : darren
 * date           : 2026. 11. 9.
 * description    : 포즈 원본 키(sourceKey, PM media_key) 규칙.
 *                  형식 검사는 API 계층의 Bean Validation(@Pattern)이 이 값으로 한다. 대소문자를 구분하고 정규화하지 않는다.
 */
object PoseSourceKey {
    const val REGEX: String = "^[A-Za-z0-9._-]{1,100}$"
}
