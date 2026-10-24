package com.neki.domain.search.infra.cache.memory

import com.neki.domain.search.external.EntityDictionaryCache
import com.neki.domain.search.models.qu.EntityDictionary
import org.springframework.stereotype.Component

/**
 * fileName       : InMemoryEntityDictionaryCacheAdapter
 * author         : koo
 * date           : 2026. 10. 4.
 * description    : NER 사전을 프로세스 메모리에 둔다. 인스턴스마다 같은 DB 에서 따로 채운다.
 */
@Component
class InMemoryEntityDictionaryCacheAdapter : EntityDictionaryCache {

    // 통째로 바꿔 끼우는 불변 사전이라 읽을 때 잠금이 필요 없다
    @Volatile
    private var dictionary: EntityDictionary = EntityDictionary(emptyList())

    override fun get(): EntityDictionary = dictionary

    override fun replace(dictionary: EntityDictionary) {
        this.dictionary = dictionary
    }
}
