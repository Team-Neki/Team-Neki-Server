package com.neki.batch.search.application

import com.neki.batch.search.application.dto.SearchIndexResult
import com.neki.core.annotation.UseCase
import com.neki.domain.map.models.Brand
import com.neki.domain.map.repository.BrandRepository
import com.neki.domain.search.models.PhotoBoothEnriched
import com.neki.domain.search.models.PhotoBoothSearchWrite
import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.repository.PhotoBoothSearchRepository
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * fileName       : SearchIndexUseCase
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : tb_photo_booth_enriched 8열로 검색 카드를 만들어 _write 에 채우고(build), _read 와 맞바꾼다(swap).
 *                  외부 호출 없이 DB 만 읽고 쓴다. search 와 map 두 도메인의 포트를 잇는 조립이라 앱 계층에 둔다
 */
@UseCase
class SearchIndexUseCase(
    private val repository: PhotoBoothSearchRepository,
    private val brandRepository: BrandRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 카드가 0건이거나 지금 서빙 중인 카드 수의 절반 미만이면 예외로 끝낸다. _write 를 비우기 전이라 직전 세대도 남는다.
     */
    @Transactional
    fun build(businessDate: LocalDate): SearchIndexResult {
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
        val result = SearchIndexResult(
            indexed = cards.size,
            stationLinks = cards.sumOf { it.stations.size },
            skippedNoCoordinate = skippedNoCoordinate,
            skippedNoBrand = skippedNoBrand.toMap(),
        )
        log.info("검색 카드 build 완료 (businessDate={}, current={}, result={})", businessDate, current, result)
        return result
    }

    /** _write 를 _read 로 올린다. 실패하면 _read 는 그대로다 */
    @Transactional
    fun swap() {
        repository.swap()
        log.info("검색 카드 swap 완료 (_write -> _read)")
    }
}
