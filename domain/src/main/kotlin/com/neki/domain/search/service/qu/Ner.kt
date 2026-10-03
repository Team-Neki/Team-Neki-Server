package com.neki.domain.search.service.qu

import com.neki.domain.search.BranchNamePolicy
import com.neki.domain.search.models.SearchTarget
import com.neki.domain.search.models.qu.DictionaryEntry
import com.neki.domain.search.models.qu.EntityDictionary
import com.neki.domain.search.models.qu.ResolvedEntity

/**
 * fileName       : Ner
 * author         : koo
 * date           : 2026. 9. 18.
 * description    : NER. QU 의 한 단계로, 정규화 검색어 안에 어떤 엔티티(브랜드·지역·역)가 사전에 있는지 찾고 겹침까지 해소한다.
 *   후보 탐색 : 검색어의 부분 문자열을 사전에서 조회한다. 사전 키보다 긴 부분 문자열은 보지 않는다.
 *   겹침 해소 : longest match. 긴 범위가 먼저, 길이가 같으면 앞선 범위가 먼저 선택되고, 선택된 범위와 겹치는 범위는 버린다.
 *     "포토이즘강남역" 은 포토이즘(브랜드), 강남(지역), 강남역(역) 이 후보이고 강남역이 강남보다 길어 이긴다.
 *   지점명 제외 : 겹침을 해소한 뒤에도 지점 접미사가 바로 뒤에 붙은 지역·역은 지점명의 일부이므로 버린다.
 *     해소 전에 거르면 "강남역점" 에서 강남역이 빠진 자리에 강남이 남아 지역으로 잡히므로 해소 뒤에 거른다.
 *   사전이 비어 있으면 아무것도 찾지 않는다.
 */
internal object Ner {

    /**
     * 서로 다른 범위는 겹치지 않고 앞선 범위부터 나온다. 한 범위에 대상이 여럿이면(e.g. 강남역의 노선들) 모두 남긴다.
     * 같은 입력과 같은 사전에는 항상 같은 결과.
     */
    fun recognize(keyword: String, dictionary: EntityDictionary): List<ResolvedEntity> =
        resolve(findCandidates(keyword, dictionary)).filterNot { isPartOfBranchName(it, keyword) }

    private fun findCandidates(text: String, dictionary: EntityDictionary): List<EntityMatch> {
        val maxKeyLength: Int = dictionary.maxKeyLength
        if (maxKeyLength == 0) return emptyList()

        val matches = mutableListOf<EntityMatch>()
        for (start in text.indices) {
            for (end in start + 1..minOf(text.length, start + maxKeyLength)) {
                val keyword: String = text.substring(start, end)
                dictionary.find(keyword).forEach { matches += EntityMatch(it, keyword, start, end) }
            }
        }
        return matches
    }

    private fun resolve(matches: List<EntityMatch>): List<ResolvedEntity> {
        val matchesBySpan: Map<Span, List<EntityMatch>> = matches.groupBy { Span(it.start, it.end) }

        val chosen = mutableListOf<Span>()
        matchesBySpan.keys
            .sortedWith(compareByDescending<Span> { it.length }.thenBy { it.start })
            .forEach { span -> if (chosen.none { it.overlaps(span) }) chosen += span }

        return chosen
            .sortedBy { it.start }
            .flatMap { span ->
                matchesBySpan.getValue(span)
                    .sortedBy { it.entry.target.toString() }
                    .map { ResolvedEntity(it.keyword, it.start, it.end, it.entry.target) }
            }
    }

    /**
     * 지역·역 바로 뒤에 지점 접미사가 붙으면 지점명의 일부다. 브랜드는 지점명에 들어가지 않고, 지점은 이미 지점이므로 보지 않는다.
     */
    private fun isPartOfBranchName(entity: ResolvedEntity, text: String): Boolean =
        entity.target is SearchTarget.Area && BranchNamePolicy.startsWithSuffix(text.substring(entity.end))

    /** 사전에서 찾은 후보. 겹치는 후보도 전부 담기며 NER 밖으로 나가지 않는다 */
    private data class EntityMatch(val entry: DictionaryEntry, val keyword: String, val start: Int, val end: Int)

    /** [start, end) */
    private data class Span(val start: Int, val end: Int) {
        val length: Int get() = end - start

        fun overlaps(other: Span): Boolean = start < other.end && other.start < end
    }
}
