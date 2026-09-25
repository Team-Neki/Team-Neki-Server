package com.neki.batch.search

import com.neki.domain.map.infra.persist.jpa.JpaBrandRepository
import com.neki.domain.map.models.Brand
import com.neki.domain.search.infra.persist.jpa.JpaPhotoBoothEnrichedRepository
import com.neki.domain.search.infra.persist.jpa.JpaPhotoBoothSearchRepository
import com.neki.domain.search.models.PhotoBoothEnriched
import com.neki.domain.search.models.PhotoBoothEnrichedId
import com.neki.domain.search.models.PhotoBoothSearch
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

/**
 * searchIndexJob 을 H2 에서 끝까지 돌린다. 역 연결은 LAZY 컬렉션이라 트랜잭션 밖에서 못 읽으므로 JdbcTemplate 로 센다
 */
@SpringBootTest
@ActiveProfiles("test")
class SearchIndexJobTest {

    @Autowired
    private lateinit var jobLauncher: JobLauncher

    @Autowired
    private lateinit var jobExplorer: JobExplorer

    @Autowired
    @Qualifier(SearchIndexJobConfig.JOB_NAME)
    private lateinit var job: Job

    @Autowired
    private lateinit var brandRepository: JpaBrandRepository

    @Autowired
    private lateinit var enrichedRepository: JpaPhotoBoothEnrichedRepository

    @Autowired
    private lateinit var searchRepository: JpaPhotoBoothSearchRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @AfterEach
    fun tearDown() {
        // 엔티티 삭제는 역 컬렉션까지 지운다. 나머지는 읽기 전용 매핑이라 배치 삭제로 충분
        searchRepository.deleteAll()
        enrichedRepository.deleteAllInBatch()
        jdbcTemplate.update("delete from tb_subway_station")
        brandRepository.deleteAllInBatch()
    }

    @Test
    fun `브랜드 접두를 뗀 지점명으로 카드가 생기고 region_ids, site_key, search_text 가 규칙대로 채워진다`() {
        givenBrand()
        givenEnriched("포토시그니처 고현점", bCode = "1168010100", address = "서울 강남구 역삼동 1")

        launch("2026-09-25").status shouldBe BatchStatus.COMPLETED

        val card: PhotoBoothSearch = searchRepository.findAll().single()
        card.branchName shouldBe "고현점"
        card.normalizedBrandName shouldBe "포토시그니처"
        card.normalizedBranchName shouldBe "고현점"
        card.searchText shouldBe "포토시그니처고현점서울강남구역삼동1"
        card.regionIds.toList() shouldContainExactly listOf("1100000000", "1168000000", "1168010100")
        card.siteKey shouldBe "1168010100:127.02760,37.49790"
        card.businessDate shouldBe LocalDate.parse("2026-09-25")
        card.sourceDt shouldBe SOURCE_DT
    }

    @Test
    fun `1km 안의 역만 연결 테이블에 들어가고 distance_m 이 있다`() {
        givenBrand()
        givenEnriched("포토시그니처 고현점")
        givenStations()

        launch("2026-09-25").status shouldBe BatchStatus.COMPLETED

        countStationLinks() shouldBe 1
        val distance: Int = jdbcTemplate.queryForObject(
            "select distance_m from tb_photo_booth_search_station where station_name = '강남' and line_name = '2호선'",
            Int::class.java,
        )!!
        distance shouldBeInRange 1..1000
    }

    @Test
    fun `b_code 가 NULL 이어도 카드가 생기고 region_ids 는 비며 역은 채워진다`() {
        givenBrand()
        givenEnriched("포토시그니처 고현점", bCode = null)
        givenStations()

        launch("2026-09-25").status shouldBe BatchStatus.COMPLETED

        val card: PhotoBoothSearch = searchRepository.findAll().single()
        card.regionIds.toList() shouldBe emptyList()
        card.siteKey shouldBe ":127.02760,37.49790"
        countStationLinks() shouldBe 1
    }

    @Test
    fun `좌표가 NULL 인 지점은 카드가 없다`() {
        givenBrand()
        givenEnriched("포토시그니처 고현점")
        givenEnriched("포토시그니처 좌표없음점", idx = "2", longitude = null, latitude = null)

        launch("2026-09-25").status shouldBe BatchStatus.COMPLETED

        searchRepository.findAll().map { it.idx } shouldContainExactly listOf("1")
    }

    @Test
    fun `platform 이 어느 브랜드에도 없으면 그 지점은 카드가 없다`() {
        givenBrand()
        givenEnriched("포토시그니처 고현점")
        givenEnriched("모르는브랜드 지점", platform = "UNKNOWN", idx = "2")

        launch("2026-09-25").status shouldBe BatchStatus.COMPLETED

        searchRepository.findAll().map { it.platform } shouldContainExactly listOf(PLATFORM)
    }

    @Test
    fun `같은 businessDate 로 두 번 돌려도 카드와 역 연결이 그대로이고 JobInstance 는 새로 생긴다`() {
        givenBrand()
        givenEnriched("포토시그니처 고현점")
        givenEnriched("포토시그니처 강남점", idx = "2", bCode = null)
        givenStations()

        val first: JobExecution = launch("2026-09-25")
        val cardsAfterFirst: Long = searchRepository.count()
        val linksAfterFirst: Int = countStationLinks()
        val siteKeysAfterFirst: List<String> = searchRepository.findAll().map { it.siteKey }

        val second: JobExecution = launch("2026-09-25")

        first.status shouldBe BatchStatus.COMPLETED
        second.status shouldBe BatchStatus.COMPLETED
        second.jobInstance.instanceId shouldNotBe first.jobInstance.instanceId
        searchRepository.count() shouldBe cardsAfterFirst
        countStationLinks() shouldBe linksAfterFirst
        searchRepository.findAll().map { it.siteKey } shouldContainExactlyInAnyOrder siteKeysAfterFirst
    }

    @Test
    fun `enriched 가 비면 job 은 FAILED 이고 직전 카드가 남는다`() {
        givenBrand()
        givenEnriched("포토시그니처 고현점")
        launch("2026-09-25").status shouldBe BatchStatus.COMPLETED
        enrichedRepository.deleteAllInBatch()

        launch("2026-09-25").status shouldBe BatchStatus.FAILED

        searchRepository.findAll().single().siteKey shouldBe "1168010100:127.02760,37.49790"
    }

    @Test
    fun `입력이 직전 카드 수의 절반 미만이면 job 은 FAILED 이고 직전 카드가 남는다`() {
        givenBrand()
        (1..3).forEach { givenEnriched("포토시그니처 ${it}호점", idx = it.toString()) }
        launch("2026-09-25").status shouldBe BatchStatus.COMPLETED
        enrichedRepository.deleteAllInBatch()
        givenEnriched("포토시그니처 1호점", idx = "1")

        launch("2026-09-25").status shouldBe BatchStatus.FAILED

        searchRepository.count() shouldBe 3
    }

    /** 기동 시 JobLauncherApplicationRunner 가 하는 것과 같은 파라미터 구성 (incrementer 적용 뒤 인자 병합) */
    private fun launch(businessDate: String): JobExecution {
        val params: JobParameters = JobParametersBuilder(jobExplorer)
            .getNextJobParameters(job)
            .addString("businessDate", businessDate)
            .toJobParameters()

        return jobLauncher.run(job, params)
    }

    private fun givenBrand() {
        brandRepository.save(Brand(name = "포토시그니처", code = "PHOTOSIGNATURE", platform = PLATFORM))
    }

    private fun givenEnriched(
        name: String,
        platform: String = PLATFORM,
        idx: String = "1",
        bCode: String? = "1168010100",
        address: String? = "서울 강남구 역삼동 1",
        longitude: Double? = 127.0276,
        latitude: Double? = 37.4979,
    ) {
        enrichedRepository.save(
            PhotoBoothEnriched(
                id = PhotoBoothEnrichedId(platform, idx),
                name = name,
                address = address,
                longitude = longitude,
                latitude = latitude,
                sourceDt = SOURCE_DT,
                bCode = bCode,
            ),
        ).shouldNotBeNull()
    }

    /** 강남(2호선) 은 지점에서 1km 안, 시청(1호선) 은 멀다. batch 테스트 classpath 에 JTS 가 없어 EWKT 로 넣는다 */
    private fun givenStations() {
        jdbcTemplate.update(
            "insert into tb_subway_station (name, line_name, location) values (?, ?, ?), (?, ?, ?)",
            "강남",
            "2호선",
            "SRID=4326;POINT(127.027926 37.497952)",
            "시청",
            "1호선",
            "SRID=4326;POINT(126.9784 37.5665)",
        )
    }

    private fun countStationLinks(): Int =
        jdbcTemplate.queryForObject("select count(*) from tb_photo_booth_search_station", Int::class.java)!!

    companion object {
        private const val PLATFORM = "PHOTO_SIGNATURE"
        private val SOURCE_DT: LocalDate = LocalDate.parse("2026-09-24")
    }
}
