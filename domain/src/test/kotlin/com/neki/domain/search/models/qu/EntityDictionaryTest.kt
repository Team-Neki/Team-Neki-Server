package com.neki.domain.search.models.qu

import com.neki.domain.search.models.LegalDong
import com.neki.domain.search.models.PhotoBoothSearch
import com.neki.domain.search.models.SearchTarget
import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.models.SubwayStationId
import com.neki.domain.search.models.qu.DictionaryEntry
import com.neki.domain.search.models.qu.EntityDictionary
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel

/**
 * fileName       : EntityDictionaryTest
 * author         : koo
 * date           : 2026. 9. 19.
 * description    : 사전 키 정규화, 같은 키의 여러 대상, 사전 이름 규칙(지역 전체 경로, 자치구 줄임말, 역 접미사·노선, 색인 지점·브랜드)
 */
class EntityDictionaryTest :
    FunSpec({

        fun brand(id: Long, name: String) = DictionaryEntry(name, SearchTarget.Brand(id))

        test("비어 있으면 찾지 못하고 키 길이가 0 이다") {
            val dictionary = EntityDictionary(emptyList())

            dictionary.find("포토이즘").shouldBeEmpty()
            dictionary.size shouldBe 0
            dictionary.maxKeyLength shouldBe 0
        }

        test("이름을 검색어와 같은 규칙으로 정규화한 키로 찾는다") {
            val dictionary = EntityDictionary(
                listOf(brand(1, "포토이즘"), brand(6, "플랜비 스튜디오"), brand(9, "Photo-Lab_Plus")),
            )

            dictionary.find("포토이즘") shouldBe listOf(brand(1, "포토이즘"))
            dictionary.find("플랜비스튜디오") shouldBe listOf(brand(6, "플랜비 스튜디오"))
            dictionary.find("photolabplus") shouldBe listOf(brand(9, "Photo-Lab_Plus"))
            dictionary.find("플랜비 스튜디오").shouldBeEmpty()
            dictionary.maxKeyLength shouldBe "photolabplus".length
        }

        test("같은 키에 다른 대상이 여럿이면 모두 남기고, 같은 대상은 하나만 남긴다") {
            val line2 = DictionaryEntry("강남역", SearchTarget.Station("강남", "2호선"))
            val sinbundang =
                DictionaryEntry("강남역", SearchTarget.Station("강남", "신분당선"))

            val dictionary = EntityDictionary(listOf(line2, sinbundang, line2))

            dictionary.find("강남역") shouldBe listOf(line2, sinbundang)
            dictionary.size shouldBe 2
        }

        test("정규화 결과가 빈 이름은 버린다") {
            EntityDictionary(listOf(brand(1, " - _ "))).size shouldBe 0
        }

        test("of - 지역은 전체 경로와 자치구 이름·줄임말, 역은 `역명역`과 `역명역 노선명`, 지점은 `브랜드명 지점명`, 브랜드는 이름으로 찾는다") {
            val point = GeometryFactory(PrecisionModel(), 4326).createPoint(Coordinate(127.0276, 37.4979))
            val monomansion: PhotoBoothSearch = mockk {
                every { platform } returns "MONOMANSION"
                every { idx } returns "m1"
                every { brandName } returns "모노맨션"
                every { branchName } returns "강남역점"
            }

            val dictionary: EntityDictionary = EntityDictionary.of(
                regions = listOf(
                    LegalDong("1168000000", 2, "강남구", "서울특별시 강남구"),
                    LegalDong("1114000000", 2, "중구", "서울특별시 중구"),
                    LegalDong("4817010300", 3, "강남동", "경상남도 진주시 강남동"),
                ),
                stations = listOf(
                    SubwayStation(SubwayStationId("강남", "2호선"), point),
                    SubwayStation(SubwayStationId("강남", "신분당선"), point),
                ),
                brandNames = mapOf(1L to "포토이즘"),
                booths = listOf(monomansion),
            )

            dictionary.find("서울특별시강남구").map { it.target } shouldBe listOf(SearchTarget.Region("1168000000"))
            dictionary.find("경상남도진주시강남동").map { it.target } shouldBe listOf(SearchTarget.Region("4817010300"))
            // 서울 자치구가 아니면 이름만으로는 찾지 않는다
            dictionary.find("강남동").shouldBeEmpty()
            dictionary.find("강남역2호선").map { it.target } shouldBe listOf(SearchTarget.Station("강남", "2호선"))
            dictionary.find("모노맨션강남역점").map { it.target } shouldBe listOf(SearchTarget.Booth("MONOMANSION", "m1"))

            dictionary.find("강남구").map { it.target } shouldBe listOf(SearchTarget.Region("1168000000"))
            dictionary.find("강남").map { it.target } shouldBe listOf(SearchTarget.Region("1168000000"))
            dictionary.find("중구").map { it.target } shouldBe listOf(SearchTarget.Region("1114000000"))
            dictionary.find("중").shouldBeEmpty()
            dictionary.find("강남역").map { it.target } shouldBe
                listOf(SearchTarget.Station("강남", "2호선"), SearchTarget.Station("강남", "신분당선"))
            dictionary.find("포토이즘").map { it.target } shouldBe listOf(SearchTarget.Brand(1))
        }
    })
