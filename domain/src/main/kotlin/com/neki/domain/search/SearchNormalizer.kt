package com.neki.domain.search

import java.util.Locale

/**
 * fileName       : SearchNormalizer
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : 검색 색인과 질의가 같이 쓰는 정규화 규칙. 색인(batch)과 검색 API 가 이 한 곳만 본다.
 */
object SearchNormalizer {

    private val STRIPPED = setOf('-', '_')
    private val WHITESPACES = Regex("\\s+")

    /** 소문자, 공백·`-`·`_` 제거. 색인과 질의가 같은 함수를 쓴다 */
    fun normalize(text: String): String = text.lowercase().filterNot { it.isWhitespace() || it in STRIPPED }

    /**
     * 앞뒤 공백을 자르고 연속 공백을 한 칸으로. 지역·역 자동완성 검색어에 쓴다.
     * 법정동·역 이름은 낱말 사이가 공백 한 칸으로 저장돼 있어, DB 값을 가공하지 않고 검색어만 맞춰야 접두 검색이 인덱스를 탄다.
     * 그래서 공백을 지우는 [normalize] 를 쓰지 않는다.
     */
    fun collapseSpaces(text: String): String = text.trim().replace(WHITESPACES, " ")

    /**
     * name 에서 브랜드명 접두를 뗀 지점명. 접두가 없으면 원문 trim. 떼고 나서 비면 원문 trim.
     * 판정은 정규화 기준이라 원문의 공백·구분자·대소문자 차이는 무시한다.
     */
    fun branchName(brandName: String, name: String): String {
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
        val rest: String = name.substring(cut).trimStart { it.isWhitespace() || it in STRIPPED }.trim()
        return rest.ifEmpty { name.trim() }
    }

    /** normalize(brandName + branchName + address) */
    fun searchText(brandName: String, branchName: String, address: String?): String =
        normalize(brandName + branchName + (address ?: ""))

    /** 법정동 코드 10자리 -> {시도, 시군구, 읍면동, 법정동} 을 중복 없이. null 이나 10자리가 아니면 빈 목록 */
    fun regionIds(bCode: String?): List<String> {
        if (bCode == null || bCode.length != 10) return emptyList()
        return listOf(
            bCode.take(2) + "0".repeat(8),
            bCode.take(5) + "0".repeat(5),
            bCode.take(8) + "00",
            bCode,
        ).distinct()
    }

    /** "<b_code>:<lon 소수 5자리>,<lat 소수 5자리>". b_code 가 null 이면 앞을 비움 (":127.02760,37.49790") */
    fun siteKey(bCode: String?, longitude: Double, latitude: Double): String =
        "${bCode ?: ""}:${"%.5f".format(Locale.ROOT, longitude)},${"%.5f".format(Locale.ROOT, latitude)}"
}
