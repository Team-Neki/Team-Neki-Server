package com.neki.api.e2e.share

import com.neki.api.e2e.E2ETestBase
import com.neki.api.testfixture.aShareLink
import com.neki.domain.media.external.MediaStorage
import com.neki.domain.media.infra.persist.jpa.JpaMediaRepository
import com.neki.domain.media.infra.storage.fake.FakeMediaStorageAdapter
import com.neki.domain.media.models.Media
import com.neki.domain.media.models.MediaStatus
import com.neki.domain.media.models.MediaType
import com.neki.domain.photo.infra.persist.jpa.JpaPhotoImageRepository
import com.neki.domain.photo.models.PhotoImage
import com.neki.domain.share.infra.persist.jpa.JpaShareLinkRepository
import com.neki.domain.share.models.ShareLink
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * fileName       : ShareE2ETestBase
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : 사진 공유 링크 E2E 테스트 Base class
 * - DB 는 E2ETestBase.truncateAllTables() 가 매 테스트 전에 비운다 (TB_SHARE_LINK 포함)
 */
abstract class ShareE2ETestBase : E2ETestBase() {

    @Autowired
    protected lateinit var photoImageRepository: JpaPhotoImageRepository

    @Autowired
    protected lateinit var mediaRepository: JpaMediaRepository

    @Autowired
    protected lateinit var shareLinkRepository: JpaShareLinkRepository

    @Autowired
    private lateinit var mediaStorage: MediaStorage

    protected val fakeStorage: FakeMediaStorageAdapter
        get() = mediaStorage as FakeMediaStorageAdapter

    @AfterEach
    override fun tearDown() {
        fakeStorage.clearTestData()
        super.tearDown()
    }

    /** photo-booth/<uuid>.jpg 키로 media 를 저장하고, UPLOADED 면 스토리지에 비어 있지 않은 바이트를 넣는다 */
    protected fun createMedia(
        ownerId: Long,
        status: MediaStatus = MediaStatus.UPLOADED,
        storageKey: String = "photo-booth/${UUID.randomUUID()}.jpg",
    ): Media {
        val media: Media = mediaRepository.save(
            Media(
                storageKey = storageKey,
                ownerId = ownerId,
                mediaType = MediaType.PHOTO_BOOTH,
                status = status,
                contentType = "image/jpeg",
            ),
        )
        fakeStorage.putTestData(storageKey, imageBytes(storageKey))
        return media
    }

    protected fun createPhoto(userId: Long, mediaId: Long): PhotoImage =
        photoImageRepository.save(PhotoImage(userId = userId, mediaId = mediaId))

    /** 사용자의 UPLOADED 사진 한 장 */
    protected fun createSharablePhoto(userId: Long): Pair<PhotoImage, Media> {
        val media: Media = createMedia(ownerId = userId)
        return createPhoto(userId = userId, mediaId = media.id!!) to media
    }

    protected fun saveShareLink(
        userId: Long,
        photoId: Long,
        expiresAt: LocalDateTime = LocalDateTime.now().plusDays(7),
        deletedAt: LocalDateTime? = null,
        token: String = randomToken(),
    ): ShareLink = shareLinkRepository.save(
        aShareLink(token = token, userId = userId, photoId = photoId, expiresAt = expiresAt, deletedAt = deletedAt),
    )

    protected fun imageBytes(storageKey: String): ByteArray = "image-bytes:$storageKey".toByteArray()

    /** 형식은 맞지만 DB 에 없는 토큰 */
    protected fun randomToken(): String = UUID.randomUUID().toString().replace("-", "").padEnd(43, 'x')

    protected fun expectedExpiresAtHeader(expiresAt: LocalDateTime): String =
        expiresAt.atZone(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"))
}
