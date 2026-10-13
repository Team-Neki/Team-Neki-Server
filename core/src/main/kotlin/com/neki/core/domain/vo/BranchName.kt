package com.neki.core.domain.vo

/**
 * fileName       : BranchName
 * author         : darren
 * date           : 2026. 10. 13.
 * description    : 수집한 지점 이름에서 브랜드명 접두를 뗀 지점명. 지도와 검색 색인이 같은 규칙을 쓴다
 *   (`포토그레이 강남2호점` → `강남2호점`)
 */
object BranchName {

    private val SEPARATORS = setOf('-', '_')

    /**
     * name 에서 브랜드명 접두를 뗀 지점명. 접두가 없으면 원문 trim. 떼고 나서 비면 원문 trim.
     * 판정은 소문자·공백·`-`·`_` 를 무시하고 비교해 원문의 공백·구분자·대소문자 차이는 무시한다.
     */
    fun of(brandName: String, name: String): String {
        // ponytail: 앞에 붙은 브랜드명만 뗀다. 접미("고현점 포토시그니처")나 영문 별칭은 다루지 않으며 필요하면 alias 표를 추가
        val brand: String = normalize(brandName)
        if (brand.isEmpty() || !normalize(name).startsWith(brand)) return name.trim()
        var acc = ""
        var cut: Int = name.length
        for (i in name.indices) {
            acc += normalize(name[i].toString())
            if (acc == brand) {
                cut = i + 1
                break
            }
        }
        val rest: String = name.substring(cut).trimStart { it.isWhitespace() || it in SEPARATORS }.trim()
        return rest.ifEmpty { name.trim() }
    }

    private fun normalize(text: String): String = text.lowercase().filterNot { it.isWhitespace() || it in SEPARATORS }
}
