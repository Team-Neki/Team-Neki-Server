package com.neki.domain.media.models

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class ImageHeaderParserTest {

    private fun png(width: Int, height: Int, chunk: String = "IHDR"): ByteArray = ByteBuffer.allocate(33)
        .put(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        .putInt(13)
        .put(chunk.toByteArray())
        .putInt(width)
        .putInt(height)
        .array()

    private fun b(vararg values: Int): ByteArray = values.map { it.toByte() }.toByteArray()

    /**
     * marker FF 위치 i 기준: length i+2..3, precision i+4, height i+5..6, width i+7..8
     */
    private fun sof(marker: Int, width: Int, height: Int): ByteArray = b(
        0xFF,
        marker,
        0x00,
        0x11,
        0x08,
        height shr 8,
        height and 0xFF,
        width shr 8,
        width and 0xFF,
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

    private val soi: ByteArray = b(0xFF, 0xD8)
    private val app0: ByteArray = b(0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 1, 1, 0, 0, 1, 0, 1, 0, 0)
    private val app1: ByteArray = b(0xFF, 0xE1, 0x00, 0x06, 0x45, 0x78, 0x69, 0x66)
    private val dqt: ByteArray = b(0xFF, 0xDB, 0x00, 0x04, 0x00, 0x01)
    private val sos: ByteArray = b(0xFF, 0xDA, 0x00, 0x08, 0x01, 0x01, 0x00, 0x00, 0x3F, 0x00)

    @Test
    @DisplayName("PNG IHDR 에서 1024x1536 을 읽는다")
    fun png() {
        ImageHeaderParser.parse(png(1024, 1536) + ByteArray(100)) shouldBe
            ImageMetadata("image/png", "png", 1024, 1536)
    }

    @Test
    @DisplayName("PNG 첫 청크가 IHDR 이 아니거나 크기가 0 이거나 2^31-1 을 넘으면 null")
    fun pngInvalid() {
        ImageHeaderParser.parse(png(10, 10, "IDAT")) shouldBe null
        ImageHeaderParser.parse(png(0, 10)) shouldBe null
        ImageHeaderParser.parse(png(-1, 10)) shouldBe null // 0xFFFFFFFF
        ImageHeaderParser.parse(png(10, 10).copyOf(20)) shouldBe null
    }

    @Test
    @DisplayName("JPEG SOF0 - 앞쪽 APPn/DQT 세그먼트를 건너뛰고 크기를 읽는다")
    fun jpegSof0() {
        ImageHeaderParser.parse(soi + app0 + app1 + dqt + sof(0xC0, 640, 480) + sos) shouldBe
            ImageMetadata("image/jpeg", "jpg", 640, 480)
    }

    @Test
    @DisplayName("JPEG SOF2(progressive) 와 마커 앞 FF 채움 바이트를 처리한다")
    fun jpegSof2WithFill() {
        ImageHeaderParser.parse(soi + app0 + b(0xFF, 0xFF, 0xFF) + sof(0xC2, 1536, 1024) + sos) shouldBe
            ImageMetadata("image/jpeg", "jpg", 1536, 1024)
    }

    @Test
    @DisplayName("JPEG 독립 마커(RSTn, TEM)는 길이 없이 넘어간다")
    fun jpegStandaloneMarkers() {
        ImageHeaderParser.parse(soi + b(0xFF, 0xD0, 0xFF, 0x01) + sof(0xC1, 100, 200)) shouldBe
            ImageMetadata("image/jpeg", "jpg", 100, 200)
    }

    @Test
    @DisplayName("C4(DHT) 는 SOF 가 아니므로 건너뛴다")
    fun dhtIsNotSof() {
        val dht: ByteArray = b(0xFF, 0xC4, 0x00, 0x09, 0x00, 0x10, 0x20, 0x00, 0x30, 0x00, 0x40)
        ImageHeaderParser.parse(soi + dht + sof(0xC0, 33, 44)) shouldBe ImageMetadata("image/jpeg", "jpg", 33, 44)
    }

    @Test
    @DisplayName("SOS 이전에 SOF 가 없으면 null")
    fun jpegNoSofBeforeSos() {
        ImageHeaderParser.parse(soi + app0 + sos + sof(0xC0, 10, 10)) shouldBe null
    }

    @Test
    @DisplayName("JPEG 이 잘렸거나 width 가 0 이거나 세그먼트 길이가 2 미만이면 null")
    fun jpegInvalid() {
        ImageHeaderParser.parse((soi + app0 + sof(0xC0, 640, 480)).copyOf(soi.size + app0.size + 6)) shouldBe null
        ImageHeaderParser.parse(soi + sof(0xC0, 0, 480)) shouldBe null
        ImageHeaderParser.parse(soi + b(0xFF, 0xE0, 0x00, 0x01) + sof(0xC0, 1, 1)) shouldBe null
        ImageHeaderParser.parse(soi + b(0xFF, 0xD9)) shouldBe null
        ImageHeaderParser.parse(soi) shouldBe null
    }

    @Test
    @DisplayName("텍스트나 빈 바이트는 null")
    fun notAnImage() {
        ImageHeaderParser.parse("media_key,head_count,tag".toByteArray()) shouldBe null
        ImageHeaderParser.parse(ByteArray(0)) shouldBe null
        ImageHeaderParser.parse(ByteArray(1024)) shouldBe null
    }
}
