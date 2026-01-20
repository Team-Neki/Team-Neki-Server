package com.yapp2app.media.infra.persist

import com.yapp2app.media.application.port.MediaRepositoryPort
import com.yapp2app.media.domain.entity.Media
import com.yapp2app.media.domain.entity.MediaStatus
import com.yapp2app.media.infra.persist.jpa.JpaMediaRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : MediaRepositoryAdapter
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:07
 * description    : Media Repository Adapter
 */
@Repository
class MediaRepositoryAdapter(private val jpaRepository: JpaMediaRepository) : MediaRepositoryPort {

    override fun getActiveMedia(ownerId: Long, id: Long): Media? =
        jpaRepository.findByOwnerIdAndIdAndStatus(ownerId, id, MediaStatus.UPLOADED)

    override fun getActiveMedias(ownerId: Long, ids: List<Long>): List<Media> =
        jpaRepository.findAllByOwnerIdAndIdInAndStatus(ownerId, ids, MediaStatus.UPLOADED)

    override fun getMediaForUploadConfirmation(ownerId: Long, id: Long): Media? =
        jpaRepository.findByOwnerIdAndIdAndStatusIn(
            ownerId,
            id,
            listOf(MediaStatus.INITIATED, MediaStatus.UPLOADED),
        )

    override fun save(media: Media): Media = jpaRepository.save(media)

    override fun delete(id: Long) {
        jpaRepository.deleteById(id)
    }

    override fun deleteAll(ids: List<Long>) {
        jpaRepository.deleteAllByIdInBatch(ids)
    }
}
