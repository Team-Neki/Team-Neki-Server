package com.neki.media.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.transaction.TransactionRunner
import com.neki.media.application.port.MediaRepositoryPort
import com.neki.media.application.port.MediaStoragePort
import com.neki.media.application.result.BackfillMediaDimensionsResult
import com.neki.media.domain.entity.Media
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/**
 * fileName       : BackfillMediaDimensionsUseCase
 * author         : darren
 * date           : 2026. 6. 18.
 * description    : width/height/size 가 null 인 기존 미디어를 S3 원본에서 계산해 채워 넣는 일회성 백필 usecase
 */
@UseCase
class BackfillMediaDimensionsUseCase(
    private val mediaRepository: MediaRepositoryPort,
    private val mediaStorage: MediaStoragePort,
    private val transactionRunner: TransactionRunner,
) {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * id cursor 로 batchSize 만큼씩 끊어가며 전체 대상 미디어를 처리한다.
     * - 대상: status = UPLOADED 이면서 width/height/size 중 하나라도 null 인 미디어
     * - size 는 S3 객체 바이트 길이, width/height 는 이미지 헤더에서 추출
     */
    fun execute(batchSize: Int = DEFAULT_BATCH_SIZE): BackfillMediaDimensionsResult {
        var lastId = 0L
        var processed = 0
        var updated = 0
        var failed = 0

        while (true) {
            val batch: List<Media> = transactionRunner.readOnly {
                mediaRepository.findMediaForDimensionBackfill(lastId, batchSize)
            }
            if (batch.isEmpty()) break

            for (media in batch) {
                processed++
                runCatching { backfillOne(media) }
                    .onSuccess { updated++ }
                    .onFailure { e ->
                        failed++
                        log.warn("미디어 백필 실패 mediaId={}, key={}", media.id, media.storageKey, e)
                    }
            }

            lastId = batch.last().id!!
        }

        log.info("미디어 dimension 백필 완료 processed={}, updated={}, failed={}", processed, updated, failed)
        return BackfillMediaDimensionsResult(processed = processed, updated = updated, failed = failed)
    }

    private fun backfillOne(media: Media) {
        val mediaId: Long = media.id!!
        val bytes: ByteArray = mediaStorage.fetchBinaryByKey(media.storageKey)
        val size: Long = bytes.size.toLong()
        val (width, height) = readDimensions(bytes)

        transactionRunner.run {
            val fresh: Media = mediaRepository.getActiveMedia(mediaId) ?: return@run
            fresh.backfillDimensions(width = width, height = height, size = size)
            mediaRepository.save(fresh)
        }

        log.info("미디어 백필 성공 mediaId={}, width={}, height={}, size={}", mediaId, width, height, size)
    }

    /**
     * 이미지 전체를 디코딩하지 않고 헤더만 읽어 width/height 추출. 디코딩 불가 시 null 반환.
     * ImageIO 가 지원하지 않는 WebP 는 RIFF 헤더를 직접 파싱한다.
     */
    private fun readDimensions(bytes: ByteArray): Pair<Int?, Int?> {
        val (width, height) = readWithImageIo(bytes)
        if (width != null && height != null) return width to height

        return readWebpDimensions(bytes) ?: (width to height)
    }

    private fun readWithImageIo(bytes: ByteArray): Pair<Int?, Int?> = runCatching {
        ByteArrayInputStream(bytes).use { input ->
            ImageIO.createImageInputStream(input).use { iis ->
                val readers = ImageIO.getImageReaders(iis)
                if (!readers.hasNext()) return null to null
                val reader = readers.next()
                try {
                    reader.input = iis
                    reader.getWidth(0) to reader.getHeight(0)
                } finally {
                    reader.dispose()
                }
            }
        }
    }.getOrDefault(null to null)

    /**
     * WebP(RIFF) 헤더에서 width/height 추출. WebP 가 아니거나 파싱 실패 시 null.
     * 포맷: "RIFF"(0..3) ____ "WEBP"(8..11) [VP8 | VP8L | VP8X](12..15)
     */
    private fun readWebpDimensions(bytes: ByteArray): Pair<Int?, Int?>? {
        if (bytes.size < 30) return null
        if (!bytes.matchesAscii(0, "RIFF") || !bytes.matchesAscii(8, "WEBP")) return null

        fun u8(i: Int): Int = bytes[i].toInt() and 0xFF

        return runCatching {
            when {
                // 단순 lossy: 0x9d 0x01 0x2a 시작코드 뒤 14bit width/height
                bytes.matchesAscii(12, "VP8 ") -> {
                    val width = ((u8(27) shl 8) or u8(26)) and 0x3FFF
                    val height = ((u8(29) shl 8) or u8(28)) and 0x3FFF
                    width to height
                }
                // lossless: signature 0x2f 뒤 14bit (width-1)/(height-1)
                bytes.matchesAscii(12, "VP8L") -> {
                    val b0 = u8(21)
                    val b1 = u8(22)
                    val b2 = u8(23)
                    val b3 = u8(24)
                    val width = 1 + (((b1 and 0x3F) shl 8) or b0)
                    val height = 1 + (((b3 and 0x0F) shl 10) or (b2 shl 2) or ((b1 and 0xC0) shr 6))
                    width to height
                }
                // extended: canvas (width-1)/(height-1) 각 24bit LE
                bytes.matchesAscii(12, "VP8X") -> {
                    val width = 1 + (u8(24) or (u8(25) shl 8) or (u8(26) shl 16))
                    val height = 1 + (u8(27) or (u8(28) shl 8) or (u8(29) shl 16))
                    width to height
                }
                else -> null
            }
        }.getOrNull()
    }

    private fun ByteArray.matchesAscii(offset: Int, marker: String): Boolean {
        if (offset + marker.length > size) return false
        return marker.indices.all { this[offset + it].toInt() == marker[it].code }
    }

    companion object {
        private const val DEFAULT_BATCH_SIZE = 100
    }
}
