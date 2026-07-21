package com.neki.map.infra.persist.jpa

import com.neki.map.entity.FavoriteMap
import com.neki.map.entity.FavoriteMapId
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaFavoriteMapRepository
 * author         : darren
 * date           : 2026. 6. 21.
 * description    :
 */
interface JpaFavoriteMapRepository : JpaRepository<FavoriteMap, FavoriteMapId>
