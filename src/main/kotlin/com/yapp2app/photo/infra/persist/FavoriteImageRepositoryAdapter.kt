package com.yapp2app.photo.infra.persist

import com.yapp2app.photo.application.port.FavoriteImageRepositoryPort
import com.yapp2app.photo.domain.entity.FavoritePhoto
import com.yapp2app.photo.domain.entity.FavoritePhotoId
import com.yapp2app.photo.infra.persist.jpa.FavoritePhotoQueryRepository
import com.yapp2app.photo.infra.persist.jpa.JpaFavoriteImageRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : FavoriteImageRepositoryAdapter
 * author         : koo
 * date           : 2026. 1. 13. 오후 9:28
 * description    :
 */
@Repository
class FavoriteImageRepositoryAdapter(
    private val jpaRepository: JpaFavoriteImageRepository,
    private val queryRepository: FavoritePhotoQueryRepository,
) : FavoriteImageRepositoryPort {

    override fun add(userId: Long, photoId: Long) {
        val id = FavoritePhotoId(userId, photoId)

        if (!jpaRepository.existsById(id)) {
            jpaRepository.save(FavoritePhoto(id))
        }
    }

    override fun delete(userId: Long, photoId: Long) = jpaRepository.deleteById(
        FavoritePhotoId(userId, photoId),
    )

    override fun deleteAll(userId: Long, photoIds: List<Long>) {
        if (photoIds.isEmpty()) return
        queryRepository.deleteAllByUserIdAndPhotoIds(userId, photoIds)
    }

    override fun exists(userId: Long, photoId: Long): Boolean = jpaRepository.existsById(
        FavoritePhotoId(userId, photoId),
    )

    override fun findPhotoIdsByUserId(userId: Long): Set<Long> = jpaRepository.findAllByIdUserId(userId)
        .map { it.id.photoId }
        .toSet()

    override fun countByUserId(userId: Long): Long = jpaRepository.countByIdUserId(userId)
}
