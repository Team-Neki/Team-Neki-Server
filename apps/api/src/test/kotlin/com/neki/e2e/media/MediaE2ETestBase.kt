package com.neki.e2e.media

import com.neki.e2e.E2ETestBase
import com.neki.media.infra.persist.jpa.JpaMediaRepository
import com.neki.media.models.Media
import com.neki.media.models.MediaStatus
import com.neki.media.models.MediaType
import com.neki.photo.infra.persist.jpa.JpaFolderRepository
import com.neki.photo.infra.persist.jpa.JpaPhotoImageFolderRepository
import com.neki.photo.infra.persist.jpa.JpaPhotoImageRepository
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired

/**
 * fileName       : MediaE2ETestBase
 * author         : koo
 * date           : 2026. 1. 20.
 * description    : Media E2E 테스트를 위한 Base class
 */
abstract class MediaE2ETestBase : E2ETestBase() {

    @Autowired
    protected lateinit var mediaRepository: JpaMediaRepository

    @Autowired
    protected lateinit var photoImageRepository: JpaPhotoImageRepository

    @Autowired
    protected lateinit var folderRepository: JpaFolderRepository

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

    protected fun createMedia(
        ownerId: Long,
        status: MediaStatus = MediaStatus.INITIATED,
        storageKey: String = "test-key-${System.currentTimeMillis()}",
        mediaType: MediaType = MediaType.PHOTO_BOOTH,
        contentType: String = "image/jpeg",
    ): Media = mediaRepository.save(
        Media(
            storageKey = storageKey,
            ownerId = ownerId,
            mediaType = mediaType,
            status = status,
            contentType = contentType,
        ),
    )
}
