package com.yapp2app.photo.infra.persist

import com.yapp2app.photo.application.port.FavoriteImageRepositoryPort
import com.yapp2app.photo.domain.entity.FavoritePhoto
import com.yapp2app.photo.domain.entity.FavoritePhotoId
import com.yapp2app.photo.infra.persist.jpa.FavoritePhotoQueryRepository
import com.yapp2app.photo.infra.persist.jpa.JpaFavoriteImageRepository
import org.springframework.dao.DataIntegrityViolationException
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
        try {
            jpaRepository.save(FavoritePhoto(userId, photoId))
        } catch (_: DataIntegrityViolationException) {
            // 이미 존재하는 경우 무시 (멱등성 고려)
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
