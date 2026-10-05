package com.neki.domain.search.models

/**
 * fileName       : CompletionKeyword
 * author         : darren
 * date           : 2026. 10. 1.
 * description    : 지역·역·부스 세 탭이 같은 검색어를 받는다. `강남 포토그레이` 처럼 지역과 브랜드를 섞어 칠 수 있어
 *                  탭마다 검색어를 낱말로 나눠 쓰는 규칙을 둔다. 검색어는 이미 공백 한 칸으로 정리돼 있다.
 */
object CompletionKeyword {

    /** 이보다 짧은 낱말은 브랜드 낱말로 보지 않는다 */
    private const val MIN_BRAND_WORD_LENGTH = 2

    fun words(keyword: String): List<String> = keyword.split(' ').filter { it.isNotEmpty() }

    /**
     * 부스 검색 조건. 낱말마다 같은 뜻의 이름 묶음이고, 묶음 안 이름 중 하나만 맞으면 그 낱말은 맞은 것이다.
     * 시도 이름은 줄임말까지 묶는다 (`서울특별시` ↔ `서울`).
     */
    fun boothTerms(keyword: String): List<List<String>> = words(keyword).map { SidoAlias.equivalents(it) }

    /**
     * 지역·역 검색어에서 브랜드 낱말을 뺀다. `강남 포토그레이` → `강남`.
     * 브랜드 이름 낱말(`플랜비 스튜디오` 의 `플랜비`, `스튜디오`)이나 브랜드 이름 전체의 앞부분이면 브랜드 낱말로 본다.
     * 낱말이 하나뿐이면 그대로 둔다. 한 낱말 검색은 지금 규칙을 그대로 따르고, 브랜드만 친 검색어는 원래 결과가 없다.
     */
    fun withoutBrandWords(keyword: String, brandNames: List<String>): String {
        val words: List<String> = words(keyword)
        if (words.size < 2) return keyword

        val brandWords: List<String> = brandNames.flatMap { words(it.lowercase()) }
        val compactBrandNames: List<String> = brandNames.map { it.lowercase().replace(" ", "") }
        return words
            .filterNot { word ->
                val lower: String = word.lowercase()
                lower.length >= MIN_BRAND_WORD_LENGTH &&
                    (brandWords.any { it.startsWith(lower) } || compactBrandNames.any { it.startsWith(lower) })
            }
            .joinToString(" ")
    }
}
