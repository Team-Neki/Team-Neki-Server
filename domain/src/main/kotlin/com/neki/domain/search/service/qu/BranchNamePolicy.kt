package com.neki.domain.search.service.qu

/**
 * fileName       : BranchNamePolicy
 * author         : koo
 * date           : 2026. 9. 19.
 * description    : 지점명 판정 규칙. 지점 접미사는 "점", "본점", "N호점" 이다.
 *   "강남점", "강남2호점", "강남역점" 처럼 지명·역명 바로 뒤에 접미사가 붙으면 그 전체가 지점명이다.
 *   정규화된 문자열(공백 없음)을 받는다. NER 과 QueryIntent 가 쓴다.
 */
internal object BranchNamePolicy {
    private val SUFFIX_AT_START = Regex("^(\\d+호점|본점|점)")
    private val SUFFIX_AT_END = Regex("(\\d+호점|본점|점)$")

    /** text 가 지점 접미사로 시작하는지. NER 이 지명·역명 바로 뒤를 볼 때 쓴다 */
    fun startsWithSuffix(text: String): Boolean = SUFFIX_AT_START.containsMatchIn(text)

    /** term 이 지점 접미사로 끝나는 지점명인지 */
    fun isBranchTerm(term: String): Boolean = SUFFIX_AT_END.containsMatchIn(term)
}
