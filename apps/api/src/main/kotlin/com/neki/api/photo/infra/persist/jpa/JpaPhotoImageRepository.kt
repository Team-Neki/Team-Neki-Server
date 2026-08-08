package com.neki.api.photo.infra.persist.jpa

import com.neki.domain.photo.models.PhotoImage
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaPhotoImageRepository
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:25
 * description    : photo image jpa repository
 */
interface JpaPhotoImageRepository : JpaRepository<PhotoImage, Long> {

    fun findByUserIdAndId(userId: Long, id: Long): PhotoImage?

    fun findAllByUserIdAndIdIn(userId: Long, ids: List<Long>): List<PhotoImage>

    fun existsByUserIdAndId(userId: Long, photoId: Long): Boolean
}
