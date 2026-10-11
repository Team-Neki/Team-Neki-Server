package com.neki.domain.search.service.qu

import com.neki.domain.search.infra.cache.memory.InMemoryEntityDictionaryCacheAdapter
import com.neki.domain.search.models.LegalDong
import com.neki.domain.search.models.PhotoBoothSearch
import com.neki.domain.search.models.SearchTarget
import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.models.SubwayStationId
import com.neki.domain.search.models.qu.DictionaryEntry
import com.neki.domain.search.models.qu.EntityDictionary
import com.neki.domain.search.models.qu.EntityType
import com.neki.domain.search.models.qu.QueryIntent
import com.neki.domain.search.models.qu.ResolvedEntity
import com.neki.domain.search.repository.LegalDongRepository
import com.neki.domain.search.repository.PhotoBoothSearchRepository
import com.neki.domain.search.repository.SubwayStationRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel

/**
 * fileName       : QueryUnderstandingServiceTest
 * author         : koo
 * date           : 2026. 9. 17.
 * description    : 메모리 사전 먼저, 범위를 못 찾으면 DB 의 자동완성 keyword(지역·역·지점), 정규화 -> NER -> Intent (엔티티, 지점, 남은 조각)
 */
class QueryUnderstandingServiceTest :
    FunSpec({

        val gangnamGu = SearchTarget.Region("1168000000")
        val gangnamLine2 = SearchTarget.Station("강남", "2호선")
        val point = GeometryFactory(PrecisionModel(), 4326).createPoint(Coordinate(127.0276, 37.4979))

        // 자동완성 keyword 로는 아무것도 찾지 못하는 저장소. 사전은 강남구, 강남역 2호선, 포토이즘
        val legalDongRepository: LegalDongRepository = mockk {
            every { findByFullName(any()) } returns null
            every { findAllBelowSido() } returns listOf(LegalDong("1168000000", 2, "강남구", "서울특별시 강남구"))
        }
        val subwayStationRepository: SubwayStationRepository = mockk {
            every { findById(any()) } returns null
            every { findAll() } returns listOf(SubwayStation(SubwayStationId("강남", "2호선"), point))
        }
        val photoBoothSearchRepository: PhotoBoothSearchRepository = mockk {
            every { findIndexedBrandNames() } returns mapOf(1L to "포토이즘")
            every { findAllCurrent() } returns emptyList()
            every { findByBoothName(any()) } returns emptyList()
        }

        val service =
            QueryUnderstandingService(
                legalDongRepository,
                subwayStationRepository,
                photoBoothSearchRepository,
                InMemoryEntityDictionaryCacheAdapter(),
            )

        val dictionary = EntityDictionary(
            listOf(
                DictionaryEntry("포토이즘", SearchTarget.Brand(1)),
                DictionaryEntry("포토랩플러스", SearchTarget.Brand(10)),
                DictionaryEntry("강남구", gangnamGu),
                DictionaryEntry("강남", gangnamGu),
                DictionaryEntry("강남역", gangnamLine2),
            ),
        )

        fun understand(raw: String): QueryIntent = service.understand(raw, dictionary)

        fun branches(intent: QueryIntent): List<String> =
            intent.entities.filter { it.type == EntityType.BRANCH }.map { it.keyword }

        test("자동완성 keyword 는 메모리 사전에서 그 하나가 되고, 범위를 찾으면 DB 를 보지 않는다") {
            val monomansion = SearchTarget.Booth(platform = "MONOMANSION", idx = "m1")
            val cache = InMemoryEntityDictionaryCacheAdapter()
            cache.replace(
                EntityDictionary(
                    listOf(
                        DictionaryEntry("포토이즘", SearchTarget.Brand(1)),
                        DictionaryEntry("강남역 2호선", gangnamLine2),
                        DictionaryEntry("경상남도 진주시 강남동", SearchTarget.Region("4817010300")),
                        DictionaryEntry("모노맨션 강남역점", monomansion),
                    ),
                ),
            )
            // 아무것도 stub 하지 않은 저장소라 DB 를 보면 예외가 난다
            val memoryOnly = QueryUnderstandingService(mockk(), mockk(), mockk(), cache)

            memoryOnly.understand("경상남도 진주시 강남동").targets shouldBe listOf(SearchTarget.Region("4817010300"))
            memoryOnly.understand("강남역 2호선").targets shouldBe listOf(gangnamLine2)
            memoryOnly.understand(" 모노맨션  강남역점 ").targets shouldBe listOf(monomansion)
            memoryOnly.understand("포토이즘 강남역 2호선").targets shouldBe listOf(SearchTarget.Brand(1), gangnamLine2)
        }

        test("사전에 없는 자동완성 keyword 는 DB 에서 정확 일치로 그 지역·역을 찾는다") {
            val legalDongs: LegalDongRepository = mockk {
                every { findByFullName("경상남도 진주시 강남동") } returns
                    LegalDong("4817010300", 3, "강남동", "경상남도 진주시 강남동")
                every { findByFullName("강남역 신분당선") } returns null
            }
            val stations: SubwayStationRepository = mockk {
                every { findById(SubwayStationId("강남", "신분당선")) } returns
                    SubwayStation(SubwayStationId("강남", "신분당선"), point)
            }
            val completion = QueryUnderstandingService(
                legalDongs,
                stations,
                photoBoothSearchRepository,
                InMemoryEntityDictionaryCacheAdapter(),
            )

            completion.understand("경상남도 진주시 강남동").targets shouldBe listOf(SearchTarget.Region("4817010300"))
            completion.understand("강남역 신분당선").targets shouldBe listOf(SearchTarget.Station("강남", "신분당선"))

            // 다른 경로와 같이 keyword 와 엔티티 범위는 정규화 검색어 기준이다
            val intent: QueryIntent = completion.understand("강남역 신분당선")
            intent.keyword shouldBe "강남역신분당선"
            intent.entities.map { it.start to it.end } shouldBe listOf(0 to "강남역신분당선".length)

            // 앞뒤·연속 공백은 QU 가 정리하고 비교한다
            completion.understand("  강남역   신분당선 ").targets shouldBe listOf(SearchTarget.Station("강남", "신분당선"))
        }

        test("사전에 없는 부스 자동완성 keyword(브랜드명 지점명)는 DB 에서 찾아 그 지점 하나로 이해한다") {
            val monomansion: PhotoBoothSearch = mockk {
                every { platform } returns "MONOMANSION"
                every { idx } returns "m1"
            }
            val booths: PhotoBoothSearchRepository = mockk {
                every { findByBoothName(any()) } returns emptyList()
                every { findByBoothName("모노맨션 강남역점") } returns listOf(monomansion)
            }
            val completion = QueryUnderstandingService(
                legalDongRepository,
                subwayStationRepository,
                booths,
                InMemoryEntityDictionaryCacheAdapter(),
            )

            val intent: QueryIntent = completion.understand(" 모노맨션  강남역점 ")
            intent.targets shouldBe listOf(SearchTarget.Booth(platform = "MONOMANSION", idx = "m1"))
            intent.entities.map { it.type } shouldBe listOf(EntityType.BRANCH)
            intent.remainingTerms.shouldBeEmpty()
        }

        test("자동완성 keyword 가 여러 종류에 해당하면 하나를 고르지 않고 모두 담는다") {
            val legalDongs: LegalDongRepository = mockk {
                every { findByFullName("강남역 2호선") } returns LegalDong("1168000000", 2, "강남구", "강남역 2호선")
            }
            val stations: SubwayStationRepository = mockk {
                every { findById(SubwayStationId("강남", "2호선")) } returns
                    SubwayStation(SubwayStationId("강남", "2호선"), point)
            }
            val both = QueryUnderstandingService(
                legalDongs,
                stations,
                photoBoothSearchRepository,
                InMemoryEntityDictionaryCacheAdapter(),
            )

            both.understand("강남역 2호선").targets shouldBe listOf(gangnamGu, gangnamLine2)
        }

        test("그 밖의 검색어는 메모리에 올린 사전으로 이해하고, 올리기 전에는 아무것도 찾지 않는다") {
            service.understand("포토이즘 강남역 2호선").targets.shouldBeEmpty()

            // 서울특별시 강남구, 강남역 2호선, 포토이즘
            service.reloadDictionary() shouldBe mapOf(
                EntityType.REGION to 1,
                EntityType.STATION to 1,
                EntityType.BRAND to 1,
                EntityType.BRANCH to 0,
            )

            service.understand("포토이즘 강남역 2호선").targets shouldBe listOf(SearchTarget.Brand(1), gangnamLine2)
            // 사전은 통합검색 후보 값만 알아 이름 조각(강남, 강남역)은 범위가 되지 않는다
            service.understand("포토이즘 강남역").targets shouldBe listOf(SearchTarget.Brand(1))
            service.understand("강남 포토이즘").targets shouldBe listOf(SearchTarget.Brand(1))
        }

        test("지역은 전체 경로로만 찾아, 동 이름 속 다른 구 이름을 지역으로 잡지 않는다") {
            val dictionary: EntityDictionary = EntityDictionary.of(
                regions = listOf(
                    LegalDong("1120000000", 2, "성동구", "서울특별시 성동구"),
                    LegalDong("1168010500", 3, "삼성동", "서울특별시 강남구 삼성동"),
                ),
                stations = emptyList(),
                brandNames = mapOf(1L to "포토이즘"),
                booths = emptyList(),
            )

            // `삼성동` 안의 `성동` 은 성동구가 아니다
            service.understand("삼성동 포토이즘", dictionary).targets shouldBe listOf(SearchTarget.Brand(1))
            service.understand("서울특별시 강남구 삼성동 포토이즘", dictionary).targets shouldBe
                listOf(SearchTarget.Region("1168010500"), SearchTarget.Brand(1))
        }

        test("엔티티가 없으면 정규화 검색어 전체가 하나의 조각이다 (한 글자여도)") {
            understand("홍대 입구").remainingTerms shouldBe listOf("홍대입구")
            understand("홍").remainingTerms shouldBe listOf("홍")
            understand("홍대").entities.shouldBeEmpty()
        }

        test("지역 + 브랜드는 단어 순서와 무관하게 같은 대상과 브랜드로 이해한다") {
            val a: QueryIntent = understand("강남구 포토이즘")
            val b: QueryIntent = understand("포토이즘 강남구")

            a.keyword shouldBe "강남구포토이즘"
            a.targets shouldBe listOf(gangnamGu, SearchTarget.Brand(1))
            b.targets.toSet() shouldBe a.targets.toSet()
            a.remainingTerms.shouldBeEmpty()
        }

        test("줄임말(강남)은 지역, 강남역은 역이다") {
            understand("강남").targets shouldBe listOf(gangnamGu)
            understand("포토이즘 강남역").targets shouldBe listOf(SearchTarget.Brand(1), gangnamLine2)
        }

        test("엔티티 앞뒤 조각은 이어 붙이지 않고 따로 남긴다") {
            val intent: QueryIntent = understand("라이브러리 포토랩플러스 강남점")

            intent.remainingTerms shouldBe listOf("라이브러리")
            branches(intent) shouldBe listOf("강남점")
            intent.targets shouldBe listOf(SearchTarget.Brand(10))
        }

        test("엔티티 사이 한 글자 조각은 버린다") {
            val intent: QueryIntent = understand("포토이즘 강남역 앞")

            intent.remainingTerms.shouldBeEmpty()
            intent.entities.map { it.type } shouldBe listOf(EntityType.BRAND, EntityType.STATION)
        }

        test("역명 뒤에 지점 접미사가 붙으면 역이 아니라 지점이다") {
            val intent: QueryIntent = understand("포토이즘 강남역 점")

            intent.entities.map { it.type } shouldBe listOf(EntityType.BRAND, EntityType.BRANCH)
            branches(intent) shouldBe listOf("강남역점")
            intent.targets shouldBe listOf(SearchTarget.Brand(1))
        }

        test("지점은 범위를 갖고 다른 엔티티와 범위 순으로 놓인다") {
            understand("라이브러리 강남점 포토랩플러스").entities shouldBe listOf(
                ResolvedEntity("라이브러리강남점", 0, 8),
                ResolvedEntity("포토랩플러스", 8, 14, SearchTarget.Brand(10)),
            )
        }
    })
