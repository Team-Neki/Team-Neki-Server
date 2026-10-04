package com.neki.domain.search.models.qu

import com.neki.domain.search.models.SearchTarget
import com.neki.domain.search.service.qu.BranchNamePolicy
import com.neki.domain.search.service.qu.SearchNormalizer

/**
 * fileName       : QueryIntent
 * author         : koo
 * date           : 2026. 9. 17.
 * description    : 검색어를 어떻게 이해했는가. 조회 조건이 아니다 (조건은 SearchCondition).
 *   keyword 는 정규화 검색어이고, 엔티티의 [start, end) 는 그 안의 문자 범위다.
 *   entities 는 인식한 브랜드·지역·역·지점과, 엔티티 사이 조각 중 지점 접미사로 끝나 지점(BRANCH)으로 분류한 것이다. 범위 순이다.
 *   remainingTerms 는 엔티티가 차지하지 않은 나머지 조각들이다. 엔티티가 없으면 검색어 전체 하나이고,
 *   엔티티가 있으면 그 사이사이 조각 중 MIN_TERM_LENGTH 이상만 남는다.
 *   만드는 길은 [of] 하나라 이 규칙이 늘 지켜진다.
 */
class QueryIntent private constructor(
    val keyword: String,
    val entities: List<ResolvedEntity>,
    val remainingTerms: List<String>,
) {

    /** 검색어가 가리킨 대상 (지역·역·브랜드) */
    val targets: List<SearchTarget>
        get() = entities.mapNotNull { it.target }.distinct()

    companion object {
        /** 엔티티 사이 조각의 최소 길이. "강남역 앞" 의 "앞" 처럼 뜻이 약한 조각은 남기지 않는다 */
        const val MIN_TERM_LENGTH = 2

        /**
         * keyword 는 SearchNormalizer 로 정규화한 검색어, recognized 는 그 안에서 인식한 서로 겹치지 않는 엔티티다.
         * 엔티티가 차지하지 않은 조각을 지점(BRANCH)과 나머지로 나눈다.
         */
        fun of(keyword: String, recognized: List<ResolvedEntity>): QueryIntent {
            require(SearchNormalizer.normalize(keyword) == keyword) { "정규화한 검색어여야 합니다: $keyword" }

            val ordered: List<ResolvedEntity> = recognized.sortedBy { it.start }
            val (branchTerms: List<Term>, remainingTerms: List<Term>) =
                terms(keyword, ordered).partition { BranchNamePolicy.isBranchTerm(it.text) }
            val branches: List<ResolvedEntity> =
                branchTerms.map { ResolvedEntity(it.text, it.start, it.start + it.text.length) }

            return QueryIntent(
                keyword = keyword,
                entities = (ordered + branches).sortedBy { it.start },
                remainingTerms = remainingTerms.map { it.text },
            )
        }

        /**
         * 엔티티가 없으면 검색어 전체를 그대로 쓴다 (길이와 무관).
         * 엔티티가 있으면 그 사이 조각 중 MIN_TERM_LENGTH 미만은 버린다.
         */
        private fun terms(text: String, entities: List<ResolvedEntity>): List<Term> {
            if (entities.isEmpty()) return listOf(Term(text, 0))

            val terms = mutableListOf<Term>()
            var cursor = 0
            entities.forEach { entity ->
                if (entity.start > cursor) terms += Term(text.substring(cursor, entity.start), cursor)
                cursor = maxOf(cursor, entity.end)
            }
            if (cursor < text.length) terms += Term(text.substring(cursor), cursor)

            return terms.filter { it.text.length >= MIN_TERM_LENGTH }
        }

        /** 엔티티가 차지하지 않은 조각. start 는 정규화 검색어 안의 시작 위치 */
        private data class Term(val text: String, val start: Int)
    }
}
