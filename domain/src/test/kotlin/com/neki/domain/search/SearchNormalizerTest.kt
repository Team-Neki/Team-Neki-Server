package com.neki.domain.search

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * fileName       : SearchNormalizerTest
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : SearchNormalizer 정규화 규칙 단위 테스트 (색인과 질의가 같은 함수를 쓴다)
 */
class SearchNormalizerTest :
    FunSpec({

        test("normalize - 소문자로 바꾸고 공백, -, _ 를 제거한다") {
            // When
            val result: String = SearchNormalizer.normalize(" Photo-Gray_홍대 점 ")

            // Then
            result shouldBe "photogray홍대점"
        }

        test("branchName - 브랜드명 접두를 떼고 지점명만 남긴다") {
            // When
            val result: String = SearchNormalizer.branchName("포토시그니처", "포토시그니처 고현점")

            // Then
            result shouldBe "고현점"
        }

        test("branchName - 원문에 공백이나 구분자가 섞여 있어도 정규화 기준으로 접두를 뗀다") {
            // When
            val result: String = SearchNormalizer.branchName("포토시그니처", "포토 시그니처-고현점")

            // Then
            result shouldBe "고현점"
        }

        test("branchName - 접두가 없으면 원문을 trim 해서 돌려준다") {
            // When
            val result: String = SearchNormalizer.branchName("포토시그니처", "서울 북촌한옥마을점")

            // Then
            result shouldBe "서울 북촌한옥마을점"
        }

        test("branchName - 떼고 나서 비면 원문을 trim 해서 돌려준다") {
            // When
            val result: String = SearchNormalizer.branchName("포토시그니처", "포토시그니처")

            // Then
            result shouldBe "포토시그니처"
        }

        test("branchName - 앞뒤 공백이 있어도 접두를 떼고 trim 한다") {
            // When
            val result: String = SearchNormalizer.branchName("인생네컷", " 인생네컷 강남역점 ")

            // Then
            result shouldBe "강남역점"
        }

        test("branchName - 영문 브랜드는 대소문자를 무시하고 접두를 뗀다") {
            // When
            val result: String = SearchNormalizer.branchName("PhotoGray", "photogray 홍대점")

            // Then
            result shouldBe "홍대점"
        }

        test("searchText - 브랜드명, 지점명, 주소를 이어 붙여 정규화한다") {
            // When
            val result: String = SearchNormalizer.searchText("포토 시그니처", "고현점", "경남 거제시 고현동 1-2")

            // Then
            result shouldBe "포토시그니처고현점경남거제시고현동12"
        }

        test("searchText - 주소가 null 이면 빈 문자열로 취급한다") {
            // When
            val result: String = SearchNormalizer.searchText("인생네컷", "강남역점", null)

            // Then
            result shouldBe "인생네컷강남역점"
        }

        test("regionIds - 법정동 코드에서 시도, 시군구, 법정동을 중복 없이 뽑는다") {
            // When
            val result: List<String> = SearchNormalizer.regionIds("1168010100")

            // Then
            result shouldBe listOf("1100000000", "1168000000", "1168010100")
        }

        test("regionIds - 리 코드가 있으면 읍면동이 하나 더 들어가 4개가 된다") {
            // When
            val result: List<String> = SearchNormalizer.regionIds("4113511101")

            // Then
            result shouldBe listOf("4100000000", "4113500000", "4113511100", "4113511101")
        }

        test("regionIds - null 이면 빈 목록") {
            // When
            val result: List<String> = SearchNormalizer.regionIds(null)

            // Then
            result.shouldBeEmpty()
        }

        test("regionIds - 10자리가 아니면 빈 목록") {
            // When
            val result: List<String> = SearchNormalizer.regionIds("12345")

            // Then
            result.shouldBeEmpty()
        }

        test("siteKey - 법정동 코드와 소수 5자리 좌표를 이어 붙인다") {
            // When
            val result: String = SearchNormalizer.siteKey("1168010100", 127.0276, 37.4979)

            // Then
            result shouldBe "1168010100:127.02760,37.49790"
        }

        test("siteKey - 법정동 코드가 null 이면 앞을 비운다") {
            // When
            val result: String = SearchNormalizer.siteKey(null, 127.0276, 37.4979)

            // Then
            result shouldBe ":127.02760,37.49790"
        }
    })
