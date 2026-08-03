package com.neki.e2e.pose

import com.neki.e2e.E2ETestBase
import com.neki.media.infra.persist.jpa.JpaMediaRepository
import com.neki.media.models.Media
import com.neki.media.models.MediaStatus
import com.neki.media.models.MediaType
import com.neki.pose.infra.cache.fake.FakePoseViewCacheAdapter
import com.neki.pose.infra.persist.jpa.JpaPoseRepository
import com.neki.pose.infra.persist.jpa.JpaScrapPoseRepository
import com.neki.pose.models.HeadCount
import com.neki.pose.models.Pose
import com.neki.pose.models.ScrapPose
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

    @Autowired
    protected lateinit var fakePoseViewCache: FakePoseViewCacheAdapter

    @AfterEach
    override fun tearDown() {
        scrapPoseRepository.deleteAllInBatch()
        poseRepository.deleteAllInBatch()
        mediaRepository.deleteAllInBatch()
        fakePoseViewCache.clearAll()
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
