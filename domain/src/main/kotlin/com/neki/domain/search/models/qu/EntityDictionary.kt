package com.neki.domain.search.models.qu

import com.neki.domain.search.SearchNormalizer
import com.neki.domain.search.models.LegalDong
import com.neki.domain.search.models.SearchTarget
import com.neki.domain.search.models.SubwayStation

/**
 * fileName       : EntityDictionary
 * author         : koo
 * date           : 2026. 9. 19.
 * description    : NER 이 쓰는 사전. 정규화한 이름 -> 사전 항목. 한 키에 항목이 여럿일 수 있다 (e.g. 강남역은 노선마다 하나).
 *   정규화 결과가 빈 이름은 버리고, 같은 키의 같은 대상은 하나만 남긴다. 만든 뒤에는 바뀌지 않는다.
 */
class EntityDictionary(entries: List<DictionaryEntry>) {

    private val entriesByKey: Map<String, List<DictionaryEntry>> = entries
        .groupBy { SearchNormalizer.normalize(it.name) }
        .filterKeys { it.isNotEmpty() }
        .mapValues { (_, group) -> group.distinctBy { it.target } }

    val size: Int = entriesByKey.values.sumOf { it.size }

    /** 브랜드 항목의 원문 이름. 지역·역 자동완성이 검색어에서 브랜드 낱말을 뺄 때 쓴다 (CompletionKeyword.withoutBrandWords) */
    val brandNames: List<String> = entries.filter { it.target is SearchTarget.Brand }.map { it.name }.distinct()

    /** 사전 키의 최대 길이. NER 이 이보다 긴 부분 문자열을 조회하지 않게 한다 */
    val maxKeyLength: Int = entriesByKey.keys.maxOfOrNull { it.length } ?: 0

    fun find(key: String): List<DictionaryEntry> = entriesByKey[key].orEmpty()

    companion object {
        private const val DISTRICT_SUFFIX = "구"

        /**
         * 사전 이름 규칙. 지원 범위는 검색 정책 1장(서울 자치구, 지하철역)을 따른다.
         * - 지역 : 자치구 이름과 줄임말 (강남구, 강남). 줄임말이 한 글자가 되면(중구 -> 중) 넣지 않는다
         * - 역 : `역명역`. 노선마다 한 항목이라 `강남역` 은 2호선과 신분당선을 모두 가리킨다
         * - 브랜드 : 검색 색인에 있는 브랜드 이름
         */
        fun of(
            districts: List<LegalDong>,
            stations: List<SubwayStation>,
            brandNames: Map<Long, String>,
        ): EntityDictionary {
            val regionEntries: List<DictionaryEntry> = districts.flatMap { district ->
                val short: String = district.leafName.removeSuffix(DISTRICT_SUFFIX)
                val names: List<String> = listOf(district.leafName) + listOf(short).filter { it.length > 1 }
                names.distinct().map { DictionaryEntry(it, SearchTarget.Region(code = district.code)) }
            }
            val stationEntries: List<DictionaryEntry> = stations.map {
                DictionaryEntry(it.nameWithSuffix, SearchTarget.Station(name = it.name, lineName = it.lineName))
            }
            val brandEntries: List<DictionaryEntry> = brandNames.map { (brandId, brandName) ->
                DictionaryEntry(brandName, SearchTarget.Brand(brandId))
            }

            return EntityDictionary(regionEntries + stationEntries + brandEntries)
        }
    }
}
