package com.neki.photo.infra.persist

import com.neki.photo.FavoriteImageRepository
import com.neki.photo.infra.persist.jpa.FavoritePhotoQueryRepository
import com.neki.photo.infra.persist.jpa.JpaFavoriteImageRepository
import com.neki.photo.models.FavoritePhoto
import com.neki.photo.models.FavoritePhotoId
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
) : FavoriteImageRepository {

    override fun add(favoritePhoto: FavoritePhoto) {
        jpaRepository.save(favoritePhoto)
    }

    override fun addAll(userId: Long, photoIds: List<Long>) {
        if (photoIds.isEmpty()) return
        val favorites = photoIds.map { photoId -> FavoritePhoto(FavoritePhotoId(userId, photoId)) }
        jpaRepository.saveAll(favorites)
    }

    override fun delete(favoritePhoto: FavoritePhoto) = jpaRepository.deleteById(favoritePhoto.id)

    override fun deleteAll(userId: Long, photoIds: List<Long>) {
        if (photoIds.isEmpty()) return
        queryRepository.deleteAllByUserIdAndPhotoIds(userId, photoIds)
    }

    override fun exists(favoritePhoto: FavoritePhoto): Boolean = jpaRepository.existsById(favoritePhoto.id)

    override fun findPhotoIdsByUserId(userId: Long): Set<Long> = jpaRepository.findAllByIdUserId(userId)
        .map { it.id.photoId }
        .toSet()

    override fun countByUserId(userId: Long): Long = jpaRepository.countByIdUserId(userId)
}
