package com.neki.domain.search.models

/**
 * fileName       : SidoAlias
 * author         : darren
 * date           : 2026. 9. 30.
 * description    : 지역 검색어 첫 낱말의 시도 줄임말·옛 이름을 법정동 원천의 시도 이름으로 바꾼다.
 *                  `서울 강남` 을 `서울특별시 강남` 으로 바꿔 전체 경로 접두 검색에 태운다.
 *                  검색어 쪽만 바꾸므로 full_name 의 text_pattern_ops 인덱스를 그대로 탄다.
 */
object SidoAlias {

    // ponytail: 시도 이름은 Workflow 가 적재하는 법정동 원천(tb_legal_dong level 1)을 옮겨 적은 것이다.
    // 원천에서 시도 이름이 바뀌면(행정구역 통합·명칭 변경) 이 표도 같이 고친다.
    private val FULL_NAMES: Map<String, String> = buildMap {
        alias("서울특별시", "서울", "서울시")
        alias("부산광역시", "부산", "부산시")
        alias("대구광역시", "대구", "대구시")
        alias("인천광역시", "인천", "인천시")
        alias("대전광역시", "대전", "대전시")
        alias("울산광역시", "울산", "울산시")
        alias("세종특별자치시", "세종", "세종시")
        alias("경기도", "경기")
        alias("충청북도", "충북")
        alias("충청남도", "충남")
        alias("경상북도", "경북")
        alias("경상남도", "경남")
        alias("강원특별자치도", "강원", "강원도")
        alias("전북특별자치도", "전북", "전라북도")
        alias("제주특별자치도", "제주", "제주도")
        // 광주광역시와 전라남도는 원천에서 한 시도로 합쳐졌다. `광주시` 는 경기도 광주시와 겹쳐 넣지 않는다
        alias("전남광주통합특별시", "광주", "광주광역시", "전남", "전라남도")
    }

    /**
     * 공백이 있고 첫 낱말이 줄임말이면 시도 이름으로 바꾼다. 공백이 없으면 그대로 둔다.
     * 전체 경로 검색에만 쓴다. 이름 검색까지 바꾸면 `광주` 로 경기도 광주시를 못 찾는다.
     */
    fun expand(keyword: String): String {
        val first: String = keyword.substringBefore(' ')
        if (first == keyword) return keyword

        val fullName: String = FULL_NAMES[first] ?: return keyword
        return fullName + keyword.substring(first.length)
    }

    /**
     * 낱말이 시도 이름이나 줄임말이면 같은 시도를 부르는 이름 전부, 아니면 낱말 하나.
     * 부스 주소는 `서울 강남구`, `강원특별자치도 춘천시` 처럼 표기가 섞여 있어 어느 쪽으로 쳐도 맞게 비교하는 데 쓴다.
     */
    fun equivalents(word: String): List<String> {
        val fullName: String = FULL_NAMES[word] ?: word.takeIf { it in FULL_NAMES.values } ?: return listOf(word)
        return listOf(fullName) + FULL_NAMES.filterValues { it == fullName }.keys
    }

    private fun MutableMap<String, String>.alias(fullName: String, vararg aliases: String) {
        aliases.forEach { put(it, fullName) }
    }
}
