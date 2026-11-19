package com.neki.domain.pose.models

/**
 * fileName       : PoseRelation
 * author         : darren
 * date           : 2026. 11. 9.
 * description    : 2인 포즈의 관계. 신규 2인 포즈에만 하나씩 붙고, 기존 포즈는 관계가 없다(null).
 *                  화면 용어의 커플은 연인을 뜻하지 않는다.
 */
enum class PoseRelation(val description: String) {
    FRIEND("친구"),
    COUPLE("커플"),
    SSUM("썸"),
    ;

    companion object {
        /**
         * PM 데이터의 tag 값을 해석한다. 앞뒤 공백과 대소문자는 무시한다.
         * 빈 값은 관계 없음(null)이고, 모르는 값은 [IllegalArgumentException] 이다.
         */
        fun fromTag(raw: String?): PoseRelation? {
            val normalized: String = raw?.trim().orEmpty()
            if (normalized.isEmpty()) return null

            return entries.firstOrNull { it.name.equals(normalized, ignoreCase = true) }
                ?: throw IllegalArgumentException("unknown pose relation tag: $normalized")
        }

        /**
         * 관계는 2인 포즈에만 붙을 수 있다.
         */
        fun isAllowedFor(headCount: HeadCount): Boolean = headCount == HeadCount.TWO
    }
}
