package com.neki.domain.media.models

/**
 * fileName       : ImageHeaderParser
 * author         : darren
 * date           : 2026. 11. 9.
 * description    : PNG/JPEG 헤더만 읽어 형식과 크기를 알아낸다.
 *                  ImageIO/AWT 를 쓰지 않으므로 alpine JRE 의 네이티브 초기화 영향이 없다.
 *                  픽셀 무결성은 검사하지 않는다. 헤더만 정상인 깨진 파일은 통과할 수 있다.
 *                  JPEG 은 저장된 크기를 그대로 읽으므로 EXIF orientation 6/8 이면 화면 기준과 가로·세로가 바뀐다.
 */
object ImageHeaderParser {

    private const val PNG_CONTENT_TYPE = "image/png"
    private const val JPEG_CONTENT_TYPE = "image/jpeg"

    private val PNG_SIGNATURE: ByteArray =
        byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val PNG_IHDR: ByteArray = byteArrayOf(0x49, 0x48, 0x44, 0x52)
    private const val PNG_HEADER_LENGTH = 24

    // SOF 마커. C4(DHT), C8(JPG 예약), CC(DAC) 는 SOF 가 아니다
    private val JPEG_SOF_MARKERS: Set<Int> =
        setOf(0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF)
    private const val JPEG_SOI = 0xD8
    private const val JPEG_EOI = 0xD9
    private const val JPEG_SOS = 0xDA
    private const val JPEG_TEM = 0x01
    private const val MARKER_PREFIX = 0xFF

    fun parse(bytes: ByteArray): ImageMetadata? = when {
        isPng(bytes) -> parsePng(bytes)
        isJpeg(bytes) -> parseJpeg(bytes)
        else -> null
    }

    private fun isPng(bytes: ByteArray): Boolean =
        bytes.size >= PNG_SIGNATURE.size && PNG_SIGNATURE.indices.all { bytes[it] == PNG_SIGNATURE[it] }

    private fun isJpeg(bytes: ByteArray): Boolean =
        bytes.size >= 2 && u8(bytes, 0) == MARKER_PREFIX && u8(bytes, 1) == JPEG_SOI

    /**
     * 시그니처 8바이트 뒤 첫 청크는 IHDR 이어야 한다. [8..11] 길이, [12..15] 'IHDR', [16..19] width, [20..23] height.
     */
    private fun parsePng(bytes: ByteArray): ImageMetadata? {
        if (bytes.size < PNG_HEADER_LENGTH) return null
        if (PNG_IHDR.indices.any { bytes[12 + it] != PNG_IHDR[it] }) return null

        val width: Long = u32(bytes, 16)
        val height: Long = u32(bytes, 20)
        if (width <= 0 || height <= 0 || width > Int.MAX_VALUE || height > Int.MAX_VALUE) return null

        return ImageMetadata(PNG_CONTENT_TYPE, "png", width.toInt(), height.toInt())
    }

    /**
     * offset 2 부터 마커를 따라간다. 마커의 FF 위치를 i 라 하면
     * length 는 i+2..i+3, (SOF 일 때) precision 은 i+4, height 는 i+5..i+6, width 는 i+7..i+8 이다.
     * 다음 마커는 i + 2 + length 에 있다.
     */
    private fun parseJpeg(bytes: ByteArray): ImageMetadata? {
        var i = 2
        while (i + 1 < bytes.size) {
            if (u8(bytes, i) != MARKER_PREFIX) return null

            // 마커 앞의 FF 채움 바이트는 건너뛴다
            if (u8(bytes, i + 1) == MARKER_PREFIX) {
                i++
                continue
            }

            val marker: Int = u8(bytes, i + 1)
            when {
                marker == JPEG_SOS || marker == JPEG_EOI -> return null
                marker == JPEG_TEM || marker in 0xD0..0xD7 -> {
                    i += 2
                    continue
                }
            }

            if (i + 3 >= bytes.size) return null
            val length: Int = u16(bytes, i + 2)
            if (length < 2) return null

            if (marker in JPEG_SOF_MARKERS) {
                if (i + 8 >= bytes.size) return null
                val height: Int = u16(bytes, i + 5)
                val width: Int = u16(bytes, i + 7)
                if (width <= 0 || height <= 0) return null
                return ImageMetadata(JPEG_CONTENT_TYPE, "jpg", width, height)
            }

            i += 2 + length
        }
        return null
    }

    private fun u8(bytes: ByteArray, index: Int): Int = bytes[index].toInt() and 0xFF

    private fun u16(bytes: ByteArray, index: Int): Int = (u8(bytes, index) shl 8) or u8(bytes, index + 1)

    private fun u32(bytes: ByteArray, index: Int): Long = (u8(bytes, index).toLong() shl 24) or
        (u8(bytes, index + 1).toLong() shl 16) or
        (u8(bytes, index + 2).toLong() shl 8) or
        u8(bytes, index + 3).toLong()
}
