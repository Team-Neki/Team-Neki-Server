package com.neki.domain.search.models.qu

import com.neki.domain.search.models.SearchTarget

/**
 * fileName       : SearchEntity
 * author         : koo
 * date           : 2026. 9. 18.
 * description    : NER 이 쓰고 돌려주는 값 객체
 */

/**
 * 검색어에서 인식하는 엔티티 종류. BRANCH 는 대상이 아니라 지점명 조각이라 가리키는 SearchTarget 이 없다.
 * QU 가 지점 접미사(점, 본점, N호점) 규칙으로 분류한다.
 */
enum class EntityType {
    BRAND,
    REGION,
    STATION,
    BRANCH,
}

/**
 * 사전 항목 하나. name 은 원문이고 사전 키는 name 을 SearchNormalizer 로 정규화한 값이다.
 */
data class DictionaryEntry(val name: String, val target: SearchTarget)

/**
 * 검색어 안에서 인식한 엔티티. [start, end) 는 정규화 검색어 안의 문자 범위.
 * 서로 다른 범위는 겹치지 않고, 한 범위에 대상이 여럿일 수 있다 (e.g. 강남역은 노선마다 하나).
 * target 이 없으면 지점(BRANCH) 조각이다.
 */
data class ResolvedEntity(val keyword: String, val start: Int, val end: Int, val target: SearchTarget? = null) {

    val type: EntityType
        get() = when (target) {
            is SearchTarget.Region -> EntityType.REGION
            is SearchTarget.Station -> EntityType.STATION
            is SearchTarget.Brand -> EntityType.BRAND
            null -> EntityType.BRANCH
        }
}
