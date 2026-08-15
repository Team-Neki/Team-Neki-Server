package com.neki.domain.photo.infra.persist

import com.neki.domain.photo.infra.persist.jpa.FavoritePhotoQueryRepository
import com.neki.domain.photo.infra.persist.jpa.JpaFavoriteImageRepository
import com.neki.domain.photo.models.FavoritePhoto
import com.neki.domain.photo.models.FavoritePhotoId
import com.neki.domain.photo.repository.FavoriteImageRepository
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
