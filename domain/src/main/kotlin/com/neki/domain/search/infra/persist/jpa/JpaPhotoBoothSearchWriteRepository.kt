package com.neki.domain.search.infra.persist.jpa

import com.neki.domain.search.models.PhotoBoothSearchWrite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

/**
 * fileName       : JpaPhotoBoothSearchWriteRepository
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : PhotoBoothSearchWrite(_write) JPA Repository
 */
interface JpaPhotoBoothSearchWriteRepository : JpaRepository<PhotoBoothSearchWrite, Long> {

    /** 카드를 지우기 전에 연결 테이블을 먼저 비운다 (H2 는 ON DELETE CASCADE 가 없다) */
    @Modifying
    @Query(value = "DELETE FROM " + PhotoBoothSearchWrite.STATION_TABLE, nativeQuery = true)
    fun deleteAllStations()
}
