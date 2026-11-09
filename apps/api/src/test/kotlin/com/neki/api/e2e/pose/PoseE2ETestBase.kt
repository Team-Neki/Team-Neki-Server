package com.neki.api.e2e.pose

import com.neki.api.e2e.E2ETestBase
import com.neki.domain.media.infra.persist.jpa.JpaMediaRepository
import com.neki.domain.media.models.Media
import com.neki.domain.media.models.MediaStatus
import com.neki.domain.media.models.MediaType
import com.neki.domain.pose.infra.persist.jpa.JpaPoseRepository
import com.neki.domain.pose.infra.persist.jpa.JpaScrapPoseRepository
import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.Pose
import com.neki.domain.pose.models.PoseRelation
import com.neki.domain.pose.models.ScrapPose
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import java.nio.ByteBuffer

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
        relation: PoseRelation? = null,
        sourceKey: String? = null,
    ): Pose = poseRepository.save(
        Pose(
            userId = userId,
            mediaId = mediaId,
            headCount = headCount,
            memo = memo,
            relation = relation,
            sourceKey = sourceKey,
        ),
    )

    protected fun createScrapPose(userId: Long, poseId: Long): ScrapPose = scrapPoseRepository.save(
        ScrapPose(userId = userId, imageId = poseId),
    )

    /**
     * PNG 시그니처 + IHDR 헤더만 있는 바이트. 서버는 헤더만 읽으므로 픽셀 데이터는 필요 없다.
     */
    protected fun pngHeaderBytes(width: Int, height: Int, padding: Int = 0): ByteArray =
        ByteBuffer.allocate(33 + padding)
            .put(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
            .putInt(13)
            .put("IHDR".toByteArray())
            .putInt(width)
            .putInt(height)
            .array()

    /**
     * SOI + SOF0 만 있는 JPEG 헤더 바이트
     */
    protected fun jpegHeaderBytes(width: Int, height: Int): ByteArray = byteArrayOf(
        0xFF.toByte(),
        0xD8.toByte(),
        0xFF.toByte(),
        0xC0.toByte(),
        0x00,
        0x11,
        0x08,
        (height shr 8).toByte(),
        height.toByte(),
        (width shr 8).toByte(),
        width.toByte(),
        0x03,
        0x01,
        0x22,
        0x00,
        0x02,
        0x11,
        0x01,
        0x03,
        0x11,
        0x01,
    )
}
