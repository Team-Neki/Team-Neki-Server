package com.neki.e2e.photo.image

import com.neki.e2e.E2ETestBase
import com.neki.media.domain.MediaType
import com.neki.media.domain.entity.Media
import com.neki.media.domain.entity.MediaStatus
import com.neki.media.infra.persist.jpa.JpaMediaRepository
import com.neki.photo.domain.entity.Folder
import com.neki.photo.domain.entity.PhotoImage
import com.neki.photo.infra.persist.jpa.JpaFavoriteImageRepository
import com.neki.photo.infra.persist.jpa.JpaFolderRepository
import com.neki.photo.infra.persist.jpa.JpaPhotoImageRepository
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

    @AfterEach
    override fun tearDown() {
        favoritePhotoRepository.deleteAllInBatch()
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

    protected fun createPhotoImage(userId: Long, mediaId: Long, folderId: Long? = null): PhotoImage =
        photoImageRepository.save(
            PhotoImage(
                userId = userId,
                mediaId = mediaId,
                folderId = folderId,
            ),
        )

    protected fun createFavoritePhotoImage(userId: Long, mediaId: Long, folderId: Long? = null): PhotoImage {
        val photo = createPhotoImage(userId, mediaId, folderId)
        favoritePhotoRepository.save(
            com.neki.photo.domain.entity.FavoritePhoto(
                userId = userId,
                imageId = photo.id!!,
            ),
        )
        return photo
    }
}
