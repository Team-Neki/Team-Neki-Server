package com.neki.domain.media.infra.persist.jpa

import com.neki.domain.media.models.Media
import com.neki.domain.media.models.MediaStatus
import org.springframework.data.jpa.repository.JpaRepository

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
}
