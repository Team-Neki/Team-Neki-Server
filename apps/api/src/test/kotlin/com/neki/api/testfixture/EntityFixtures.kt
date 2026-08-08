package com.neki.api.testfixture

import com.neki.domain.map.models.Brand
import com.neki.domain.map.models.PhotoBoothLocation
import com.neki.domain.media.models.Media
import com.neki.domain.media.models.MediaStatus
import com.neki.domain.media.models.MediaType
import com.neki.domain.photo.models.FavoritePhoto
import com.neki.domain.photo.models.Folder
import com.neki.domain.photo.models.PhotoImage
import com.neki.domain.photo.models.PhotoImageFolder
import com.neki.domain.photo.models.UploadMethod
import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.Pose
import com.neki.domain.pose.models.ScrapPose
import com.neki.domain.support.models.AppVersion
import com.neki.domain.support.models.Platform
import com.neki.domain.support.models.Term
import com.neki.domain.support.models.TermType
import com.neki.domain.support.models.UserTermAgreement
import com.neki.domain.user.models.ProviderType
import com.neki.domain.user.models.RoleType
import com.neki.domain.user.models.User
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import java.time.LocalDateTime

// ── User ─────────────────────────────────────────────────────────────────────

fun aUser(
    id: Long? = 1L,
    email: String? = "test@example.com",
    password: String = "NO_PASS",
    name: String? = "테스트유저",
    oid: String = id?.let { "oauth-oid-$it" } ?: "oauth-oid-${java.util.UUID.randomUUID()}",
    providerType: ProviderType = ProviderType.KAKAO,
    profileImageId: Long? = null,
    roles: String = RoleType.USER.role,
): User = User(
    id = id,
    email = email,
    password = password,
    oid = oid,
    name = name,
    providerType = providerType,
    profileImageId = profileImageId,
    roles = roles,
)

// ── AppVersion ────────────────────────────────────────────────────────────────

fun anAppVersion(
    id: Long = 1L,
    platform: Platform = Platform.IOS,
    minVersion: String = "1.0.0",
    currentVersion: String = "1.2.0",
): AppVersion = AppVersion(
    id = id,
    platform = platform,
    minVersion = minVersion,
    currentVersion = currentVersion,
)

// ── Term ──────────────────────────────────────────────────────────────────────

fun aTerm(
    id: Long = 1L,
    termType: TermType = TermType.SERVICE,
    title: String = "서비스 이용약관",
    url: String = "https://example.com/terms/service",
    version: String = "1.0.0",
    isRequired: Boolean = true,
    isActive: Boolean = true,
    displayOrder: Int = 0,
): Term = Term(
    id = id,
    termType = termType,
    title = title,
    url = url,
    version = version,
    isRequired = isRequired,
    isActive = isActive,
    displayOrder = displayOrder,
)

fun aUserTermAgreement(
    userId: Long = 1L,
    termId: Long = 1L,
    agreedAt: LocalDateTime = LocalDateTime.of(2025, 1, 1, 0, 0),
    termVersion: String = "1.0.0",
): UserTermAgreement = UserTermAgreement(
    userId = userId,
    termId = termId,
    agreedAt = agreedAt,
    termVersion = termVersion,
)

// ── Media ─────────────────────────────────────────────────────────────────────

fun aMedia(
    id: Long? = 1L,
    storageKey: String = "pose/test-image-${id ?: java.util.UUID.randomUUID()}.jpg",
    ownerId: Long = 1L,
    mediaType: MediaType = MediaType.POSE,
    status: MediaStatus = MediaStatus.UPLOADED,
    contentType: String = "image/jpeg",
    width: Int? = 800,
    height: Int? = 600,
    size: Long? = 102400L,
): Media = Media(
    id = id,
    storageKey = storageKey,
    ownerId = ownerId,
    mediaType = mediaType,
    status = status,
    contentType = contentType,
    width = width,
    height = height,
    size = size,
)

// ── Folder ────────────────────────────────────────────────────────────────────

fun aFolder(id: Long = 1L, userId: Long = 1L, name: String = "테스트 폴더"): Folder = Folder(
    id = id,
    userId = userId,
    name = name,
)

// ── PhotoImage ────────────────────────────────────────────────────────────────

fun aPhotoImage(
    id: Long = 1L,
    userId: Long = 1L,
    mediaId: Long = 1L,
    memo: String? = null,
    uploadMethod: UploadMethod? = UploadMethod.DIRECT_UPLOAD,
    deletedAt: LocalDateTime? = null,
    capturedAt: LocalDateTime? = null,
): PhotoImage = PhotoImage(
    id = id,
    userId = userId,
    mediaId = mediaId,
    memo = memo,
    uploadMethod = uploadMethod,
    deletedAt = deletedAt,
    capturedAt = capturedAt,
)

// ── FavoritePhoto ─────────────────────────────────────────────────────────────

fun aFavoritePhoto(userId: Long = 1L, photoId: Long = 1L): FavoritePhoto = FavoritePhoto(
    userId = userId,
    imageId = photoId,
)

// ── PhotoImageFolder ──────────────────────────────────────────────────────────

fun aPhotoImageFolder(id: Long = 1L, photoImageId: Long = 1L, folderId: Long = 1L): PhotoImageFolder = PhotoImageFolder(
    id = id,
    photoImageId = photoImageId,
    folderId = folderId,
)

// ── Pose ──────────────────────────────────────────────────────────────────────

fun aPose(
    id: Long = 1L,
    userId: Long? = 1L,
    mediaId: Long = 1L,
    headCount: HeadCount = HeadCount.TWO,
    memo: String? = null,
    viewCount: Long = 0L,
): Pose = Pose(
    id = id,
    userId = userId,
    mediaId = mediaId,
    headCount = headCount,
    memo = memo,
    viewCount = viewCount,
)

// ── ScrapPose ─────────────────────────────────────────────────────────────────

fun aScrapPose(userId: Long = 1L, poseId: Long = 1L): ScrapPose = ScrapPose(
    userId = userId,
    imageId = poseId,
)

// ── Brand ─────────────────────────────────────────────────────────────────────

fun aBrand(id: Long = 1L, name: String = "인생네컷", code: String = "lifefour", mediaId: Long? = null): Brand = Brand(
    id = id,
    name = name,
    code = code,
    mediaId = mediaId,
)

// ── PhotoBoothLocation ────────────────────────────────────────────────────────

private val GEOMETRY_FACTORY = GeometryFactory(PrecisionModel(), 4326)

fun aPhotoBoothLocation(
    id: Long = 1L,
    mapId: String = "map-$id",
    brandId: Long = 1L,
    branchName: String = "강남점",
    address: String = "서울특별시 강남구 테헤란로 123",
    longitude: Double = 127.0276,
    latitude: Double = 37.4979,
): PhotoBoothLocation {
    val point = GEOMETRY_FACTORY.createPoint(Coordinate(longitude, latitude))
    return PhotoBoothLocation(
        id = id,
        mapId = mapId,
        brandId = brandId,
        branchName = branchName,
        address = address,
        location = point,
    )
}
