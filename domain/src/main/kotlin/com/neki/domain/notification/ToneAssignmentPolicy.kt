package com.neki.domain.notification

import com.neki.domain.notification.models.MessageTone
import java.time.LocalDate

/**
 * fileName       : ToneAssignmentPolicy
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : 발송 건마다 결정적으로 톤을 배정한다. 같은 유저라도 발송일이 바뀌면 순환한다.
 *                  두 항을 각각 mod 한 뒤 더하는 이유는 userId 가 Long.MAX_VALUE 근처면 그냥 더할 때 오버플로가 나기 때문
 */
object ToneAssignmentPolicy {

    fun assign(userId: Long, businessDate: LocalDate): MessageTone = MessageTone.entries[
        Math.floorMod(Math.floorMod(userId, 3) + Math.floorMod(businessDate.toEpochDay(), 3), 3),
    ]
}
