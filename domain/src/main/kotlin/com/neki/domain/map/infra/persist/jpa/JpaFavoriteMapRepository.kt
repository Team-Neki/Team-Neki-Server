package com.neki.domain.map.infra.persist.jpa

import com.neki.domain.map.models.FavoriteMap
import com.neki.domain.map.models.FavoriteMapId
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaFavoriteMapRepository
 * author         : darren
 * date           : 2026. 6. 21.
 * description    :
 */
interface JpaFavoriteMapRepository : JpaRepository<FavoriteMap, FavoriteMapId>
