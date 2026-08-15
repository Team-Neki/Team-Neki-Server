package com.neki.domain.media.infra.persist

import com.neki.domain.media.infra.persist.jpa.JpaMediaRepository
import com.neki.domain.media.models.Media
import com.neki.domain.media.models.MediaStatus
import com.neki.domain.media.repository.MediaRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : MediaRepositoryAdapter
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:07
 * description    : Media Repository Adapter
 */
@Repository
class MediaRepositoryAdapter(private val jpaRepository: JpaMediaRepository) : MediaRepository {

    override fun getActiveMedia(id: Long): Media? = jpaRepository.findByIdAndStatus(id, MediaStatus.UPLOADED)

    override fun getActiveMedia(ownerId: Long, id: Long): Media? =
        jpaRepository.findByOwnerIdAndIdAndStatus(ownerId, id, MediaStatus.UPLOADED)

    override fun getActiveMedias(ids: List<Long>): List<Media> =
        jpaRepository.findAllByIdInAndStatus(ids, MediaStatus.UPLOADED)

    override fun getActiveMedias(ownerId: Long, ids: List<Long>): List<Media> =
        jpaRepository.findAllByOwnerIdAndIdInAndStatus(ownerId, ids, MediaStatus.UPLOADED)

    override fun getMediaForUploadConfirmation(ownerId: Long, ids: List<Long>): List<Media> =
        jpaRepository.findAllByOwnerIdAndIdInAndStatusIn(
            ownerId,
            ids,
            listOf(MediaStatus.INITIATED, MediaStatus.UPLOADED),
        )

    override fun save(media: Media): Media = jpaRepository.save(media)

    override fun saveAll(medias: List<Media>): List<Media> = jpaRepository.saveAll(medias)

    override fun delete(id: Long) {
        jpaRepository.deleteById(id)
    }

    override fun deleteAll(ids: List<Long>) {
        jpaRepository.deleteAllByIdInBatch(ids)
    }
}
