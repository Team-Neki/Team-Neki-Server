package com.neki.domain.search.infra.persist

import com.neki.domain.search.infra.persist.jpa.JpaPhotoBoothEnrichedRepository
import com.neki.domain.search.infra.persist.jpa.JpaPhotoBoothSearchRepository
import com.neki.domain.search.infra.persist.jpa.JpaPhotoBoothSearchWriteRepository
import com.neki.domain.search.infra.persist.jpa.JpaSubwayStationRepository
import com.neki.domain.search.models.PhotoBoothEnriched
import com.neki.domain.search.models.PhotoBoothSearch
import com.neki.domain.search.models.PhotoBoothSearchWrite
import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.repository.PhotoBoothSearchRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

/**
 * fileName       : PhotoBoothSearchRepositoryAdapter
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : PhotoBoothSearchRepository 의 JPA + JDBC 어댑터. 이름 맞바꾸기(DDL)만 JdbcTemplate 로 한다
 */
@Repository
class PhotoBoothSearchRepositoryAdapter(
    private val readRepository: JpaPhotoBoothSearchRepository,
    private val writeRepository: JpaPhotoBoothSearchWriteRepository,
    private val enrichedRepository: JpaPhotoBoothEnrichedRepository,
    private val stationRepository: JpaSubwayStationRepository,
    private val jdbcTemplate: JdbcTemplate,
) : PhotoBoothSearchRepository {

    // 운영은 PostgreSQL, 테스트는 H2. lock_timeout 과 ANALYZE 문법만 다르다
    private val postgres: Boolean by lazy {
        jdbcTemplate.dataSource!!.connection.use { it.metaData.databaseProductName == "PostgreSQL" }
    }

    override fun findAllEnriched(): List<PhotoBoothEnriched> = enrichedRepository.findAll()

    override fun findAllStations(): List<SubwayStation> = stationRepository.findAll()

    override fun countCurrent(): Long = readRepository.count()

    // 트랜잭션은 호출자(SearchIndexUseCase)가 연다. 여기서 열면 비우기와 채우기가 한 트랜잭션이라는 보장이 흐려진다
    override fun replaceWrite(cards: List<PhotoBoothSearchWrite>) {
        writeRepository.deleteAllStations()
        writeRepository.deleteAllInBatch()
        writeRepository.saveAll(cards)
        // 역 연결(ElementCollection)은 flush 시점에 INSERT 되므로 통계를 내기 전에 밀어낸다.
        // 갓 채운 테이블은 통계가 비어 있어 맞바꾼 직후 첫 질의가 GIN/GIST 대신 seq scan 을 고를 수 있다
        writeRepository.flush()
        jdbcTemplate.execute(
            if (postgres) "ANALYZE ${PhotoBoothSearchWrite.TABLE}" else "ANALYZE TABLE ${PhotoBoothSearchWrite.TABLE}",
        )
    }

    /**
     * 두 테이블이 같은 이름을 동시에 가질 수 없어 _tmp 를 거쳐 셋이 돌아간다. 한 트랜잭션이라 밖에서는 _tmp 가 보이지 않는다.
     * RENAME 은 ACCESS EXCLUSIVE 락이라 긴 조회 하나가 물려 있으면 뒤따르는 검색 요청까지 줄을 세운다.
     * 그래서 락을 LOCK_TIMEOUT 만 기다리고 실패한다. 실패해도 _read 는 그대로이고 다시 돌리면 된다.
     * 인덱스·제약 이름은 테이블 객체를 따라가므로 이름이 오가도 부딪히지 않는다 (V33 이 슬롯 a/b 로 지었다).
     */
    override fun swap() {
        jdbcTemplate.execute(
            if (postgres) "SET LOCAL lock_timeout = '$LOCK_TIMEOUT'" else "SET LOCK_TIMEOUT $LOCK_TIMEOUT_MILLIS",
        )
        rotate(PhotoBoothSearch.TABLE, PhotoBoothSearchWrite.TABLE, TMP_TABLE)
        rotate(PhotoBoothSearch.STATION_TABLE, PhotoBoothSearchWrite.STATION_TABLE, TMP_STATION_TABLE)
    }

    private fun rotate(read: String, write: String, tmp: String) {
        jdbcTemplate.execute("ALTER TABLE $read RENAME TO $tmp")
        jdbcTemplate.execute("ALTER TABLE $write RENAME TO $read")
        jdbcTemplate.execute("ALTER TABLE $tmp RENAME TO $write")
    }

    companion object {
        private const val LOCK_TIMEOUT = "1s"
        private const val LOCK_TIMEOUT_MILLIS = 1000
        private const val TMP_TABLE = "tb_photo_booth_search_tmp"
        private const val TMP_STATION_TABLE = "tb_photo_booth_search_tmp_station"
    }
}
