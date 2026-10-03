package com.neki.domain.search.service.qu

import com.neki.domain.search.models.SearchTarget
import com.neki.domain.search.models.qu.DictionaryEntry
import com.neki.domain.search.models.qu.EntityDictionary
import com.neki.domain.search.models.qu.EntityType
import com.neki.domain.search.models.qu.ResolvedEntity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * fileName       : NerTest
 * author         : koo
 * date           : 2026. 9. 18.
 * description    : 부분 문자열 후보 탐색과 longest match 겹침 해소
 */
class NerTest :
    FunSpec({

        fun recognize(text: String, vararg entries: DictionaryEntry): List<ResolvedEntity> =
            Ner.recognize(text, EntityDictionary(entries.toList()))

        val gangnamGu = SearchTarget.Region("1168000000")
        val gangnamLine2 = SearchTarget.Station("강남", "2호선")
        val gangnamSinbundang = SearchTarget.Station("강남", "신분당선")

        val photoism = DictionaryEntry("포토이즘", SearchTarget.Brand(1))
        val lifeFourCuts = DictionaryEntry("인생네컷", SearchTarget.Brand(2))
        val photoLabPlus = DictionaryEntry("포토랩플러스", SearchTarget.Brand(10))
        val gangnam = DictionaryEntry("강남", gangnamGu)
        val gangnamStation = DictionaryEntry("강남역", gangnamLine2)

        test("사전이 비어 있으면 아무것도 찾지 않는다") {
            recognize("포토이즘강남").shouldBeEmpty()
        }

        test("검색어 앞, 중간, 뒤 어디에 있어도 브랜드를 찾는다") {
            recognize("포토이즘강남", photoism, photoLabPlus) shouldBe
                listOf(ResolvedEntity("포토이즘", 0, 4, SearchTarget.Brand(1)))
            recognize("강남포토이즘", photoism, photoLabPlus) shouldBe
                listOf(ResolvedEntity("포토이즘", 2, 6, SearchTarget.Brand(1)))
            recognize("라이브러리포토랩플러스강남점", photoism, photoLabPlus) shouldBe
                listOf(ResolvedEntity("포토랩플러스", 5, 11, SearchTarget.Brand(10)))
        }

        test("여러 엔티티는 앞선 범위부터 나온다") {
            recognize("인생네컷포토이즘", photoism, lifeFourCuts) shouldBe listOf(
                ResolvedEntity("인생네컷", 0, 4, SearchTarget.Brand(2)),
                ResolvedEntity("포토이즘", 4, 8, SearchTarget.Brand(1)),
            )
        }

        test("겹치는 후보는 긴 쪽을 고른다 (강남 < 강남역)") {
            recognize("포토랩플러스강남역", photoLabPlus, gangnam, gangnamStation) shouldBe listOf(
                ResolvedEntity("포토랩플러스", 0, 6, SearchTarget.Brand(10)),
                ResolvedEntity("강남역", 6, 9, gangnamLine2),
            )
        }

        test("길이가 같은 겹침은 앞선 범위를 고른다") {
            val ab = DictionaryEntry("ab", SearchTarget.Region("1"))
            val bc = DictionaryEntry("bc", SearchTarget.Region("2"))

            recognize("abc", bc, ab) shouldBe
                listOf(ResolvedEntity("ab", 0, 2, SearchTarget.Region("1")))
        }

        test("같은 범위에 대상이 여럿이면 모두 남긴다 (역은 노선마다 하나)") {
            val sinbundang = DictionaryEntry("강남역", gangnamSinbundang)

            recognize("강남역", sinbundang, gangnamStation) shouldBe listOf(
                ResolvedEntity("강남역", 0, 3, gangnamLine2),
                ResolvedEntity("강남역", 0, 3, gangnamSinbundang),
            )
        }

        test("사전 입력 순서와 무관하게 같은 결과") {
            val text = "포토랩플러스강남역"

            recognize(text, gangnamStation, gangnam, photoLabPlus) shouldBe
                recognize(text, photoLabPlus, gangnam, gangnamStation)
        }

        test("강남은 지역, 강남역은 역, 강남점은 지점이라 엔티티가 아니다") {
            val entries = arrayOf(gangnam, gangnamStation, photoism)

            recognize("강남", *entries).map { it.type } shouldBe listOf(EntityType.REGION)
            recognize("강남역", *entries).map { it.type } shouldBe listOf(EntityType.STATION)
            recognize("강남점", *entries).shouldBeEmpty()
            recognize("강남2호점", *entries).shouldBeEmpty()
            recognize("강남본점", *entries).shouldBeEmpty()
            recognize("강남역점", *entries).shouldBeEmpty()
        }

        test("지점 접미사 규칙은 지역·역에만 적용되고 브랜드는 그대로 인식한다") {
            recognize("포토이즘강남점", gangnam, photoism).map { it.type to it.keyword } shouldBe
                listOf(EntityType.BRAND to "포토이즘")
            recognize("강남포토이즘", gangnam, photoism).map { it.type } shouldBe
                listOf(EntityType.REGION, EntityType.BRAND)
            recognize("포토이즘점", gangnam, photoism).map { it.type } shouldBe listOf(EntityType.BRAND)
        }
    })
