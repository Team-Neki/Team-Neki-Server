package com.neki.photo.models

/**
 * fileName       : MediaAvailability
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : media가 object storage에 실제로 저장되었는지 여부
 */
enum class MediaAvailability {
    AVAILABLE,
    UNAVAILABLE,
}

/**
 * 업로드 확인 요청 하나에 대한 media별 가용 여부 묶음.
 */
class MediaAvailabilities(private val byMediaId: Map<Long, MediaAvailability>) {

    /** 하나라도 스토리지에 없으면 그 업로드는 실패로 본다. */
    val hasUnavailable: Boolean
        get() = byMediaId.values.any { it != MediaAvailability.AVAILABLE }

    /** 정상 업로드된 media. 실패 시 되돌릴 대상이다. */
    val availableMediaIds: List<Long>
        get() = byMediaId.filterValues { it == MediaAvailability.AVAILABLE }.keys.toList()
}
