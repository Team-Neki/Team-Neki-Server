package com.neki.domain.search.infra.persist.jpa

import com.neki.domain.search.models.PhotoBoothSearch
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

/**
 * fileName       : JpaPhotoBoothSearchRepository
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : PhotoBoothSearch JPA Repository
 */
interface JpaPhotoBoothSearchRepository : JpaRepository<PhotoBoothSearch, Long> {

    /** H2 는 ON DELETE CASCADE 가 없으므로 카드를 지우기 전에 연결 테이블을 먼저 비운다 */
    @Modifying
    @Query(value = "DELETE FROM tb_photo_booth_search_station", nativeQuery = true)
    fun deleteAllStations()
}
