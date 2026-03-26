package com.neki.e2e.photo.folder

import com.neki.e2e.E2ETestBase
import com.neki.media.domain.MediaType
import com.neki.media.domain.entity.Media
import com.neki.media.domain.entity.MediaStatus
import com.neki.media.infra.persist.jpa.JpaMediaRepository
import com.neki.photo.domain.entity.PhotoImage
import com.neki.photo.domain.entity.PhotoImageFolder
import com.neki.photo.infra.persist.jpa.JpaFolderRepository
import com.neki.photo.infra.persist.jpa.JpaPhotoImageFolderRepository
import com.neki.photo.infra.persist.jpa.JpaPhotoImageRepository
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

/**
 * fileName       : FolderE2ETestBase
 * author         : koo
 * date           : 2025. 12. 29. 오전 2:43
 * description    :
 */
abstract class FolderE2ETestBase : E2ETestBase() {

    @Autowired
    protected lateinit var folderRepository: JpaFolderRepository

    @Autowired
    protected lateinit var photoImageRepository: JpaPhotoImageRepository

    @Autowired
    protected lateinit var mediaRepository: JpaMediaRepository

    @Autowired
    protected lateinit var photoImageFolderRepository: JpaPhotoImageFolderRepository

    @AfterEach
    override fun tearDown() {
        photoImageFolderRepository.deleteAllInBatch()
        photoImageRepository.deleteAllInBatch()
        folderRepository.deleteAllInBatch()
        mediaRepository.deleteAllInBatch()
        super.tearDown()
    }

    protected fun createMedia(ownerId: Long): Media = mediaRepository.save(
        Media(
            storageKey = "test-storage-key-${UUID.randomUUID()}",
            ownerId = ownerId,
            mediaType = MediaType.PHOTO_BOOTH,
            status = MediaStatus.UPLOADED,
            contentType = "image/jpeg",
        ),
    )

    protected fun createPhotoImage(userId: Long, mediaId: Long, folderId: Long? = null): PhotoImage {
        val photo = photoImageRepository.save(
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
}
