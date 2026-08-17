package com.neki.domain.photo.models

/**
 * fileName       : PhotoWithFavorite
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 사진과 즐겨찾기 여부를 함께 담은 조회 결과
 */
data class PhotoWithFavorite(val photo: PhotoImage, val isFavorite: Boolean)
