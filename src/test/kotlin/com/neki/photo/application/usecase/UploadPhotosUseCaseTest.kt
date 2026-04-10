package com.neki.photo.application.usecase

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.photo.application.command.UploadPhotoCommand
import com.neki.photo.application.contract.MediaAvailability
import com.neki.photo.application.port.FavoriteImageRepositoryPort
import com.neki.photo.application.port.FolderRepositoryPort
import com.neki.photo.application.port.MediaClientPort
import com.neki.photo.application.port.PhotoImageFolderRepositoryPort
import com.neki.photo.application.port.PhotoImageRepositoryPort
import com.neki.photo.domain.enums.UploadMethod
import com.neki.testfixture.FakeTransactionRunner
import com.neki.testfixture.aFolder
import com.neki.testfixture.aPhotoImage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UploadPhotosUseCaseTest {

    lateinit var mediaClient: MediaClientPort
    lateinit var photoImageRepository: PhotoImageRepositoryPort
    lateinit var photoImageFolderRepository: PhotoImageFolderRepositoryPort
    lateinit var folderRepository: FolderRepositoryPort
    lateinit var favoriteImageRepository: FavoriteImageRepositoryPort
    lateinit var useCase: UploadPhotosUseCase

    @BeforeEach
    fun setUp() {
        mediaClient = mockk()
        photoImageRepository = mockk()
        photoImageFolderRepository = mockk()
        folderRepository = mockk()
        favoriteImageRepository = mockk()
        useCase = UploadPhotosUseCase(
            mediaClient,
            photoImageRepository,
            photoImageFolderRepository,
            folderRepository,
            favoriteImageRepository,
            FakeTransactionRunner(),
        )
    }

    private fun makeUploadItem(mediaId: Long, memo: String? = null) = UploadPhotoCommand.UploadItem(
        mediaId = mediaId,
        uploadMethod = UploadMethod.DIRECT_UPLOAD,
        memo = memo,
        capturedAt = null,
    )

    @Test
    @DisplayName("정상 업로드 - 미디어 확인 후 사진 저장, 폴더 연결, 즐겨찾기 추가")
    fun `정상 업로드 - 미디어 확인 후 사진 저장, 폴더 연결, 즐겨찾기 추가`() {
        // Given
        val uploads = listOf(makeUploadItem(10L), makeUploadItem(20L))
        val command = UploadPhotoCommand(userId = 1L, folderId = 1L, uploads = uploads, favorite = true)
        val folder = aFolder(id = 1L, userId = 1L)
        val savedPhotos = listOf(
            aPhotoImage(id = 100L, userId = 1L, mediaId = 10L),
            aPhotoImage(id = 200L, userId = 1L, mediaId = 20L),
        )

        every { folderRepository.getOwnedFolder(1L, 1L) } returns folder
        every { photoImageRepository.getRegisteredMediaIds(listOf(10L, 20L)) } returns emptySet()
        every { mediaClient.verifyMediasUploaded(1L, listOf(10L, 20L)) } returns mapOf(
            10L to MediaAvailability.AVAILABLE,
            20L to MediaAvailability.AVAILABLE,
        )
        every { photoImageRepository.saveAll(any()) } returns savedPhotos
        every { photoImageFolderRepository.saveAll(listOf(100L, 200L), 1L) } just Runs
        every { favoriteImageRepository.addAll(1L, listOf(100L, 200L)) } just Runs

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { photoImageRepository.saveAll(any()) }
        verify(exactly = 1) { photoImageFolderRepository.saveAll(listOf(100L, 200L), 1L) }
        verify(exactly = 1) { favoriteImageRepository.addAll(1L, listOf(100L, 200L)) }
    }

    @Test
    @DisplayName("중복 mediaId가 있는 경우 INVALID_PARAMETER 예외 발생")
    fun `중복 mediaId가 있는 경우 INVALID_PARAMETER 예외 발생`() {
        // Given
        val uploads = listOf(makeUploadItem(10L), makeUploadItem(10L))
        val command = UploadPhotoCommand(userId = 1L, folderId = null, uploads = uploads, favorite = false)

        // When & Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.INVALID_PARAMETER
        verify(exactly = 0) { photoImageRepository.saveAll(any()) }
    }

    @Test
    @DisplayName("폴더를 소유하지 않은 경우 NOT_FOUND 예외 발생")
    fun `폴더를 소유하지 않은 경우 NOT_FOUND 예외 발생`() {
        // Given
        val uploads = listOf(makeUploadItem(10L))
        val command = UploadPhotoCommand(userId = 1L, folderId = 99L, uploads = uploads, favorite = false)

        every { folderRepository.getOwnedFolder(1L, 99L) } returns null

        // When & Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.NOT_FOUND
        verify(exactly = 0) { photoImageRepository.saveAll(any()) }
    }

    @Test
    @DisplayName("일부 미디어가 UNAVAILABLE인 경우 UPLOAD_FAILED 예외 발생 및 성공 미디어 롤백")
    fun `일부 미디어가 UNAVAILABLE인 경우 UPLOAD_FAILED 예외 발생 및 성공 미디어 롤백`() {
        // Given
        val uploads = listOf(makeUploadItem(10L), makeUploadItem(20L))
        val command = UploadPhotoCommand(userId = 1L, folderId = null, uploads = uploads, favorite = false)

        every { photoImageRepository.getRegisteredMediaIds(listOf(10L, 20L)) } returns emptySet()
        every { mediaClient.verifyMediasUploaded(1L, listOf(10L, 20L)) } returns mapOf(
            10L to MediaAvailability.AVAILABLE,
            20L to MediaAvailability.UNAVAILABLE,
        )
        every { mediaClient.rollbackMediasUploaded(1L, listOf(10L)) } just Runs

        // When & Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.UPLOAD_FAILED
        verify(exactly = 1) { mediaClient.rollbackMediasUploaded(1L, listOf(10L)) }
    }

    @Test
    @DisplayName("트랜잭션 실패 시 미디어 롤백 호출")
    fun `트랜잭션 실패 시 미디어 롤백 호출`() {
        // Given
        val uploads = listOf(makeUploadItem(10L))
        val command = UploadPhotoCommand(userId = 1L, folderId = null, uploads = uploads, favorite = false)

        every { photoImageRepository.getRegisteredMediaIds(listOf(10L)) } returns emptySet()
        every { mediaClient.verifyMediasUploaded(1L, listOf(10L)) } returns
            mapOf(10L to MediaAvailability.AVAILABLE)
        every { photoImageRepository.saveAll(any()) } throws RuntimeException("DB 저장 실패")
        every { mediaClient.rollbackMediasUploaded(1L, listOf(10L)) } just Runs

        // When & Then
        shouldThrow<RuntimeException> {
            useCase.execute(command)
        }
        verify(exactly = 1) { mediaClient.rollbackMediasUploaded(1L, listOf(10L)) }
    }

    @Test
    @DisplayName("이미 등록된 mediaId는 필터링 후 나머지만 저장")
    fun `이미 등록된 mediaId는 필터링 후 나머지만 저장`() {
        // Given
        val uploads = listOf(makeUploadItem(10L), makeUploadItem(20L))
        val command = UploadPhotoCommand(userId = 1L, folderId = null, uploads = uploads, favorite = false)
        // mediaId=10L은 이미 등록됨
        val savedPhotos = listOf(aPhotoImage(id = 200L, userId = 1L, mediaId = 20L))

        every { photoImageRepository.getRegisteredMediaIds(listOf(10L, 20L)) } returns setOf(10L)
        every { mediaClient.verifyMediasUploaded(1L, listOf(20L)) } returns
            mapOf(20L to MediaAvailability.AVAILABLE)
        every { photoImageRepository.saveAll(any()) } returns savedPhotos

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { mediaClient.verifyMediasUploaded(1L, listOf(20L)) }
        verify(exactly = 1) { photoImageRepository.saveAll(match { it.size == 1 && it[0].mediaId == 20L }) }
    }

    @Test
    @DisplayName("모든 mediaId가 이미 등록된 경우 early return - 저장 미호출")
    fun `모든 mediaId가 이미 등록된 경우 early return - 저장 미호출`() {
        // Given
        val uploads = listOf(makeUploadItem(10L), makeUploadItem(20L))
        val command = UploadPhotoCommand(userId = 1L, folderId = null, uploads = uploads, favorite = false)

        every { photoImageRepository.getRegisteredMediaIds(listOf(10L, 20L)) } returns setOf(10L, 20L)

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 0) { mediaClient.verifyMediasUploaded(any(), any()) }
        verify(exactly = 0) { photoImageRepository.saveAll(any()) }
    }

    @Test
    @DisplayName("folderId가 null이면 폴더 연결 없이 사진만 저장")
    fun `folderId가 null이면 폴더 연결 없이 사진만 저장`() {
        // Given
        val uploads = listOf(makeUploadItem(10L))
        val command = UploadPhotoCommand(userId = 1L, folderId = null, uploads = uploads, favorite = false)
        val savedPhotos = listOf(aPhotoImage(id = 100L, userId = 1L, mediaId = 10L))

        every { photoImageRepository.getRegisteredMediaIds(listOf(10L)) } returns emptySet()
        every { mediaClient.verifyMediasUploaded(1L, listOf(10L)) } returns
            mapOf(10L to MediaAvailability.AVAILABLE)
        every { photoImageRepository.saveAll(any()) } returns savedPhotos

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { photoImageRepository.saveAll(any()) }
        verify(exactly = 0) { photoImageFolderRepository.saveAll(any(), any()) }
    }

    @Test
    @DisplayName("saveAll에서 ALREADY_REQUEST 예외 발생 시 롤백 없이 early return")
    fun `saveAll에서 ALREADY_REQUEST 예외 발생 시 롤백 없이 early return`() {
        // Given
        val uploads = listOf(makeUploadItem(10L))
        val command = UploadPhotoCommand(userId = 1L, folderId = null, uploads = uploads, favorite = false)

        every { photoImageRepository.getRegisteredMediaIds(listOf(10L)) } returns emptySet()
        every { mediaClient.verifyMediasUploaded(1L, listOf(10L)) } returns
            mapOf(10L to MediaAvailability.AVAILABLE)
        every { photoImageRepository.saveAll(any()) } throws BusinessException(ResultCode.ALREADY_REQUEST)

        // When - 예외 없이 정상 종료
        useCase.execute(command)

        // Then
        verify(exactly = 0) { mediaClient.rollbackMediasUploaded(any(), any()) }
    }

    @Test
    @DisplayName("롤백 중 예외 발생 시 롤백 예외가 전파됨")
    fun `롤백 중 예외 발생 시 롤백 예외가 전파됨`() {
        // Given
        val uploads = listOf(makeUploadItem(10L))
        val command = UploadPhotoCommand(userId = 1L, folderId = null, uploads = uploads, favorite = false)
        val originalException = RuntimeException("원래 오류")
        val rollbackException = RuntimeException("롤백 오류")

        every { photoImageRepository.getRegisteredMediaIds(listOf(10L)) } returns emptySet()
        every { mediaClient.verifyMediasUploaded(1L, listOf(10L)) } returns
            mapOf(10L to MediaAvailability.AVAILABLE)
        every { photoImageRepository.saveAll(any()) } throws originalException
        every { mediaClient.rollbackMediasUploaded(1L, listOf(10L)) } throws rollbackException

        // When & Then - 롤백 중 예외가 발생하면 롤백 예외가 전파됨 (원래 예외는 마스킹됨)
        val ex = shouldThrow<RuntimeException> {
            useCase.execute(command)
        }
        ex shouldBe rollbackException
        verify(exactly = 1) { mediaClient.rollbackMediasUploaded(1L, listOf(10L)) }
    }
}
