package com.neki.domain.photo.models

/**
 * fileName       : FolderStats
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 폴더와 그 안의 사진 수, 대표 이미지를 함께 담은 조회 결과
 */
data class FolderStats(val folderId: Long, val name: String, val coverImageStorageKey: String?, val photoCount: Long)
