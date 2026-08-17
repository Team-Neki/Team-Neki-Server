package com.neki.api.e2e.photo.image

import com.neki.api.e2e.E2ETestBase
import com.neki.domain.media.infra.persist.jpa.JpaMediaRepository
import com.neki.domain.media.models.Media
import com.neki.domain.media.models.MediaStatus
import com.neki.domain.media.models.MediaType
import com.neki.domain.photo.infra.persist.jpa.JpaFavoriteImageRepository
import com.neki.domain.photo.infra.persist.jpa.JpaFolderRepository
import com.neki.domain.photo.infra.persist.jpa.JpaPhotoImageFolderRepository
import com.neki.domain.photo.infra.persist.jpa.JpaPhotoImageRepository
import com.neki.domain.photo.models.FavoritePhoto
import com.neki.domain.photo.models.Folder
import com.neki.domain.photo.models.PhotoImage
import com.neki.domain.photo.models.PhotoImageFolder
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

/**
 * fileName       : PhotoImageE2ETestBase
 * author         : koo
 * date           : 2026. 1. 8. 오후 7:42
 * description    : PhotoImage E2E 테스트를 위한 Base class
 */
abstract class PhotoImageE2ETestBase : E2ETestBase() {

    @Autowired
    protected lateinit var folderRepository: JpaFolderRepository

    @Autowired
    protected lateinit var photoImageRepository: JpaPhotoImageRepository

    @Autowired
    protected lateinit var mediaRepository: JpaMediaRepository

    @Autowired
    protected lateinit var favoritePhotoRepository: JpaFavoriteImageRepository

    @Autowired
    protected lateinit var photoImageFolderRepository: JpaPhotoImageFolderRepository

    @AfterEach
    override fun tearDown() {
        favoritePhotoRepository.deleteAllInBatch()
        photoImageFolderRepository.deleteAllInBatch()
        photoImageRepository.deleteAllInBatch()
        folderRepository.deleteAllInBatch()
        mediaRepository.deleteAllInBatch()
        super.tearDown()
    }

    protected fun createFolder(userId: Long, name: String = "테스트 폴더"): Folder = folderRepository.save(
        Folder(
            userId = userId,
            name = name,
        ),
    )

    protected fun createMedia(
        ownerId: Long,
        status: MediaStatus = MediaStatus.UPLOADED,
        mediaType: MediaType = MediaType.PHOTO_BOOTH,
        contentType: String = "image/jpeg",
    ): Media = mediaRepository.save(
        Media(
            storageKey = "test-storage-key-${UUID.randomUUID()}",
            ownerId = ownerId,
            mediaType = mediaType,
            status = status,
            contentType = contentType,
        ),
    )

    protected fun createPhotoImage(userId: Long, mediaId: Long, folderId: Long? = null): PhotoImage {
        val photo: PhotoImage = photoImageRepository.save(
            PhotoImage(
                userId = userId,
                mediaId = mediaId,
            ),
        )
        if (folderId != null) {
            photoImageFolderRepository.save(PhotoImageFolder(photoImageId = photo.id!!, folderId = folderId))
        }
        return photo
    }

    protected fun createFavoritePhotoImage(userId: Long, mediaId: Long, folderId: Long? = null): PhotoImage {
        val photo = createPhotoImage(userId, mediaId, folderId)
        favoritePhotoRepository.save(
            FavoritePhoto(
                userId = userId,
                imageId = photo.id!!,
            ),
        )
        return photo
    }
}
