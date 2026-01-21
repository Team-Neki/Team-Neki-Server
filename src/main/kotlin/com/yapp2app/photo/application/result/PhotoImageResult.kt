package com.yapp2app.photo.application.result

/**
 * fileName       : PhotoImageResult
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:28
 * description    : photo image application 결과
 */
data class UploadPhotoResult(val photoId: Long)

data class GetPhotosResult(val photos: List<PhotoInfo>, val hasNext: Boolean) {

    data class PhotoInfo(
        val photoId: Long,
        val imageBinary: ByteArray,
        val folderId: Long?,
        val contentType: String,
        val createdAt: String,
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PhotoInfo) return false
            return photoId == other.photoId
        }

        override fun hashCode(): Int = photoId.hashCode()
    }
}
