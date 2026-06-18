package com.neki.media.infra.persist.jpa

import com.neki.media.domain.entity.Media
import com.neki.media.domain.entity.MediaStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * fileName       : JpaMediaRepository
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:08
 * description    : media jpa repository
 */
interface JpaMediaRepository : JpaRepository<Media, Long> {

    fun findByOwnerIdAndIdAndStatus(ownerId: Long, id: Long, status: MediaStatus): Media?

    fun findByIdAndStatus(id: Long, status: MediaStatus): Media?

    fun findAllByOwnerIdAndIdInAndStatusIn(ownerId: Long, ids: List<Long>, statuses: List<MediaStatus>): List<Media>

    fun findAllByIdInAndStatus(ids: List<Long>, status: MediaStatus): List<Media>

    fun findAllByOwnerIdAndIdInAndStatus(ownerId: Long, ids: List<Long>, status: MediaStatus): List<Media>

    /**
     * width/height/size 중 하나라도 null 인 미디어를 id 기준 cursor 로 조회한다. (백필용)
     */
    @Query(
        """
        select m from Media m
        where m.status = :status
          and m.id > :lastId
          and (m.width is null or m.height is null or m.size is null)
        order by m.id asc
        """,
    )
    fun findForDimensionBackfill(
        @Param("status") status: MediaStatus,
        @Param("lastId") lastId: Long,
        pageable: Pageable,
    ): List<Media>
}
