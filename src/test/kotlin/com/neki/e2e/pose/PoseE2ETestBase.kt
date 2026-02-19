package com.neki.e2e.pose

import com.neki.e2e.E2ETestBase
import com.neki.media.domain.MediaType
import com.neki.media.domain.entity.Media
import com.neki.media.domain.entity.MediaStatus
import com.neki.media.infra.persist.jpa.JpaMediaRepository
import com.neki.pose.domain.HeadCount
import com.neki.pose.domain.entity.Pose
import com.neki.pose.domain.entity.ScrapPose
import com.neki.pose.infra.persist.jpa.JpaPoseRepository
import com.neki.pose.infra.persist.jpa.JpaScrapPoseRepository
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired

/**
 * fileName       : PoseE2ETestBase
 * author         : claude
 * date           : 2026. 1. 29.
 * description    : Pose E2E 테스트를 위한 Base class
 */
abstract class PoseE2ETestBase : E2ETestBase() {

    @Autowired
    protected lateinit var poseRepository: JpaPoseRepository

    @Autowired
    protected lateinit var scrapPoseRepository: JpaScrapPoseRepository

    @Autowired
    protected lateinit var mediaRepository: JpaMediaRepository

    @AfterEach
    override fun tearDown() {
        scrapPoseRepository.deleteAllInBatch()
        poseRepository.deleteAllInBatch()
        mediaRepository.deleteAllInBatch()
        super.tearDown()
    }

    protected fun createMedia(
        ownerId: Long,
        status: MediaStatus = MediaStatus.UPLOADED,
        mediaType: MediaType = MediaType.POSE,
        contentType: String = "image/jpeg",
    ): Media = mediaRepository.save(
        Media(
            storageKey = "test-storage-key-${System.currentTimeMillis()}",
            ownerId = ownerId,
            mediaType = mediaType,
            status = status,
            contentType = contentType,
        ),
    )

    protected fun createPose(
        userId: Long,
        mediaId: Long,
        headCount: HeadCount = HeadCount.ONE,
        memo: String? = null,
    ): Pose = poseRepository.save(
        Pose(
            userId = userId,
            mediaId = mediaId,
            headCount = headCount,
            memo = memo,
        ),
    )

    protected fun createScrapPose(userId: Long, poseId: Long): ScrapPose = scrapPoseRepository.save(
        ScrapPose(userId = userId, imageId = poseId),
    )
}
