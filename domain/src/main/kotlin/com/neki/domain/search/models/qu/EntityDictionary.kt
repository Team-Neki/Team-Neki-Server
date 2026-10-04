package com.neki.domain.search.models.qu

import com.neki.domain.search.models.LegalDong
import com.neki.domain.search.models.PhotoBoothSearch
import com.neki.domain.search.models.SearchTarget
import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.service.qu.SearchNormalizer

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

    private val brandEntries: List<DictionaryEntry> = entries.filter { it.target is SearchTarget.Brand }

    /** 브랜드 항목의 원문 이름. 지역·역 자동완성이 검색어에서 브랜드 낱말을 뺄 때 쓴다 (CompletionKeyword.withoutBrandWords) */
    val brandNames: List<String> = brandEntries.map { it.name }.distinct()

    /**
     * 브랜드 항목만 담은 사전. 전체 사전에서는 `포토이즘 강남점` 이 지점 항목으로 통째로 잡혀(longest match) 그 안의 브랜드가 나오지 않으므로,
     * 검색어에 적힌 브랜드만 찾을 때는 이 사전으로 NER 을 돌린다. 처음 쓸 때 만든다
     */
    val brandsOnly: EntityDictionary by lazy { EntityDictionary(brandEntries) }

    /** 사전 키의 최대 길이. NER 이 이보다 긴 부분 문자열을 조회하지 않게 한다 */
    val maxKeyLength: Int = entriesByKey.keys.maxOfOrNull { it.length } ?: 0

    fun find(key: String): List<DictionaryEntry> = entriesByKey[key].orEmpty()

    companion object {
        private const val DISTRICT_SUFFIX = "구"

        /**
         * 사전 이름 규칙. 자유 검색어의 지원 범위는 검색 정책 1장(서울 자치구, 지하철역)을 따르고,
         * 자동완성 keyword(지역 전체 경로, `역명역 노선명`, 부스 `브랜드명 지점명`)도 넣어 NER 이 longest match 로 그 하나를 고르게 한다.
         * - 지역 : 시군구 이하 법정동의 전체 경로(서울특별시 강남구). 서울 자치구는 이름과 줄임말도 (강남구, 강남). 줄임말이 한 글자가 되면(중구 -> 중) 넣지 않는다
         * - 역 : `역명역` 과 `역명역 노선명`. 노선마다 한 항목이라 `강남역` 은 2호선과 신분당선을 모두, `강남역 2호선` 은 2호선만 가리킨다
         * - 지점 : 검색 색인의 `브랜드명 지점명`
         * - 브랜드 : 검색 색인에 있는 브랜드 이름
         */
        fun of(
            regions: List<LegalDong>,
            stations: List<SubwayStation>,
            brandNames: Map<Long, String>,
            booths: List<PhotoBoothSearch>,
        ): EntityDictionary {
            val regionEntries: List<DictionaryEntry> = regions.flatMap { region ->
                val short: String = region.leafName.removeSuffix(DISTRICT_SUFFIX)
                val districtNames: List<String> =
                    listOf(region.leafName, short).filter { region.isSeoulDistrict && it.length > 1 }
                (listOf(region.fullName) + districtNames).map {
                    DictionaryEntry(it, SearchTarget.Region(code = region.code))
                }
            }
            val stationEntries: List<DictionaryEntry> = stations.flatMap {
                val target = SearchTarget.Station(name = it.name, lineName = it.lineName)
                listOf(DictionaryEntry(it.nameWithSuffix, target), DictionaryEntry(it.keyword, target))
            }
            val boothEntries: List<DictionaryEntry> = booths.map {
                DictionaryEntry("${it.brandName} ${it.branchName}", SearchTarget.Booth(it.platform, it.idx))
            }
            val brandEntries: List<DictionaryEntry> = brandNames.map { (brandId, brandName) ->
                DictionaryEntry(brandName, SearchTarget.Brand(brandId))
            }

            return EntityDictionary(regionEntries + stationEntries + boothEntries + brandEntries)
        }
    }
}
