package com.neki.photo.contract

import com.neki.photo.domain.entity.PhotoImage

/**
 * fileName       : PhotoWithFavorite
 * author         : koo
 * date           : 2026. 1. 25. 오후 12:29
 * description    :
 */
data class PhotoWithFavorite(val photo: PhotoImage, val isFavorite: Boolean)
