package com.yapp2app.e2e.photo.folder

import com.yapp2app.e2e.E2ETestBase
import com.yapp2app.media.domain.MediaType
import com.yapp2app.media.domain.entity.Media
import com.yapp2app.media.domain.entity.MediaStatus
import com.yapp2app.media.infra.persist.jpa.JpaMediaRepository
import com.yapp2app.photo.domain.entity.PhotoImage
import com.yapp2app.photo.infra.persist.jpa.JpaFolderRepository
import com.yapp2app.photo.infra.persist.jpa.JpaPhotoImageRepository
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

    @AfterEach
    override fun tearDown() {
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

    protected fun createPhotoImage(userId: Long, mediaId: Long, folderId: Long? = null): PhotoImage =
        photoImageRepository.save(
            PhotoImage(
                userId = userId,
                mediaId = mediaId,
                folderId = folderId,
            ),
        )
}
