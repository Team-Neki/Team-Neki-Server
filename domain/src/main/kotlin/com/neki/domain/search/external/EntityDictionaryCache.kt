package com.neki.domain.search.external

import com.neki.domain.search.models.qu.EntityDictionary

/**
 * fileName       : EntityDictionaryCache
 * author         : koo
 * date           : 2026. 10. 4.
 * description    : NER 사전 보관소. 앱이 뜰 때와 10분마다 통째로 바꿔 끼우고(QueryUnderstandingService.reloadDictionary) 요청마다 꺼내 쓴다.
 *   지금은 프로세스 메모리에 두고, 여러 인스턴스가 사전 하나를 나눠 써야 하면 Redis 어댑터로 바꿔 끼운다.
 */
interface EntityDictionaryCache {

    /** 지금 사전. 채우기 전에는 빈 사전이라 NER 이 아무것도 찾지 않는다 */
    fun get(): EntityDictionary

    /** 사전을 통째로 바꾼다 */
    fun replace(dictionary: EntityDictionary)
}
