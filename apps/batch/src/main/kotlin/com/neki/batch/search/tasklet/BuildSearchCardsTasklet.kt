package com.neki.batch.search.tasklet

import com.neki.batch.search.job.SearchIndexJobConfig
import com.neki.domain.map.models.Brand
import com.neki.domain.map.repository.BrandRepository
import com.neki.domain.search.models.PhotoBoothEnriched
import com.neki.domain.search.models.PhotoBoothSearchWrite
import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.repository.PhotoBoothSearchRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * fileName       : BuildSearchCardsTasklet
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : tb_photo_booth_enriched 8열로 검색 카드를 만들어 _write 에 채운다. 외부 호출 없이 DB 만 읽고 쓴다.
 *                  search 와 map 두 도메인의 포트를 잇는 조립이라 앱 계층(batch)에 둔다.
 *
 * TaskletStep 이 execute() 를 step 트랜잭션 안에서 돌리므로 비우기와 채우기가 한 트랜잭션이다. 별도 트랜잭션 선언은 두지 않는다.
 * 카드가 0건이거나 서빙 중(_read) 카드의 절반 미만이면 _write 를 건드리기 전에 예외로 끝낸다. 직전 세대가 그대로 남는다.
 * 예외는 그대로 올려 step FAILED -> job FAILED -> 종료 코드 0 아님 (BACKEND-128 계약).
 */
@Component
class BuildSearchCardsTasklet(
    private val repository: PhotoBoothSearchRepository,
    private val brandRepository: BrandRepository,
) : Tasklet {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val businessDate: LocalDate = businessDate(chunkContext)

        // tb_brand.platform 이 NULL 인 브랜드는 수집 대상이 아니므로 매핑에서 뺀다
        val brands: Map<String, Brand> = brandRepository.findAll()
            .filter { it.platform != null }
            .associateBy { it.platform!! }
        val enriched: List<PhotoBoothEnriched> = repository.findAllEnriched()
        val stations: List<SubwayStation> = repository.findAllStations()
        val indexedAt = LocalDateTime.now()

        val skippedNoBrand = mutableMapOf<String, Int>()
        var skippedNoCoordinate = 0
        val cards: List<PhotoBoothSearchWrite> = enriched.mapNotNull { row ->
            val brand: Brand? = brands[row.id.platform]
            if (brand == null) {
                skippedNoBrand.merge(row.id.platform, 1, Int::plus)
                return@mapNotNull null
            }
            if (row.coordinateOrNull() == null) {
                skippedNoCoordinate++
                return@mapNotNull null
            }
            PhotoBoothSearchWrite.of(row, brand.id!!, brand.name, brand.code, stations, businessDate, indexedAt)
        }

        val current: Long = repository.countCurrent()
        check(cards.isNotEmpty()) { "색인할 지점이 없습니다. enriched 가 비어 있거나 전부 건너뛰었습니다 (서빙 중 카드 ${current}건)" }
        check(cards.size * 2 >= current) { "색인 건수 ${cards.size}건이 서빙 중 카드 ${current}건의 절반 미만이라 교체하지 않습니다" }

        repository.replaceWrite(cards)

        skippedNoBrand.forEach { (platform, count) ->
            log.warn("tb_brand.platform 에 없는 platform 이라 건너뜀 (platform={}, count={})", platform, count)
        }
        // BATCH_STEP_EXECUTION 의 write/filter count 로도 남긴다
        contribution.incrementWriteCount(cards.size.toLong())
        contribution.incrementFilterCount((skippedNoCoordinate + skippedNoBrand.values.sum()).toLong())
        log.info(
            "{} 완료 (businessDate={}, indexed={}, stationLinks={}, skippedNoCoordinate={}, skippedNoBrand={}, current={})",
            SearchIndexStepConfig.BUILD_STEP_NAME,
            businessDate,
            cards.size,
            cards.sumOf { it.stations.size },
            skippedNoCoordinate,
            skippedNoBrand,
            current,
        )
        return RepeatStatus.FINISHED
    }

    private fun businessDate(chunkContext: ChunkContext): LocalDate {
        val raw: Any =
            requireNotNull(chunkContext.stepContext.jobParameters[SearchIndexJobConfig.PARAM_BUSINESS_DATE]) {
                "${SearchIndexJobConfig.PARAM_BUSINESS_DATE} 파라미터가 없습니다 (예: ${SearchIndexJobConfig.PARAM_BUSINESS_DATE}=2026-09-25)"
            }
        return LocalDate.parse(raw.toString())
    }
}
