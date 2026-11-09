package com.neki.api.pose.api.dto

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PoseCsvParserTest {

    private fun bytes(text: String): ByteArray = text.toByteArray(Charsets.UTF_8)

    private fun shouldBeInvalid(block: () -> Unit) {
        shouldThrow<BusinessException> { block() }.resultCode shouldBe ResultCode.INVALID_PARAMETER
    }

    @Test
    @DisplayName("CRLF + 빈 tag - PM pose.csv 형식을 그대로 읽는다")
    fun parsesCrlf() {
        val rows = PoseCsvParser.parsePoseCsv(
            bytes("media_key,head_count,tag\r\ncouple_001_F1-4_M1-5,TWO,couple\r\nsolo_001_F2-3,ONE,\r\n"),
        )

        rows shouldBe listOf(
            PoseCsvParser.PoseRow(2, "couple_001_F1-4_M1-5", "TWO", "couple"),
            PoseCsvParser.PoseRow(3, "solo_001_F2-3", "ONE", ""),
        )
    }

    @Test
    @DisplayName("LF, BOM, 빈 줄, 헤더 순서가 달라도 된다. 줄 번호는 물리 줄 기준이다")
    fun parsesLfBomAndReorderedHeader() {
        val rows = PoseCsvParser.parsePoseCsv(
            bytes("\uFEFFtag, Head_Count ,media_key\n\nfriend, two ,friend_001\n"),
        )

        rows shouldBe listOf(PoseCsvParser.PoseRow(3, "friend_001", "two", "friend"))
    }

    @Test
    @DisplayName("뒤쪽 열이 빠진 행은 빈 값으로 읽는다")
    fun missingTrailingColumnIsEmpty() {
        val rows = PoseCsvParser.parsePoseCsv(bytes("media_key,head_count,tag\nsolo_001,ONE\n"))

        rows.single().tag shouldBe ""
    }

    @Test
    @DisplayName("필수 헤더 누락은 INVALID_PARAMETER")
    fun missingHeader() = shouldBeInvalid {
        PoseCsvParser.parsePoseCsv(bytes("media_key,tag\nfriend_001,friend\n"))
    }

    @Test
    @DisplayName("데이터 행이 0개면 INVALID_PARAMETER")
    fun noDataRows() = shouldBeInvalid {
        PoseCsvParser.parsePoseCsv(bytes("media_key,head_count,tag\r\n\r\n"))
    }

    @Test
    @DisplayName("빈 파일은 INVALID_PARAMETER")
    fun emptyFile() = shouldBeInvalid {
        PoseCsvParser.parsePoseCsv(ByteArray(0))
    }

    @Test
    @DisplayName("데이터 행이 1000개를 넘으면 INVALID_PARAMETER, 1000개는 허용")
    fun tooManyRows() {
        val header = "media_key,head_count,tag\n"
        val thousand: String = (1..1000).joinToString("") { "k_$it,TWO,friend\n" }

        PoseCsvParser.parsePoseCsv(bytes(header + thousand)).size shouldBe 1000
        shouldBeInvalid { PoseCsvParser.parsePoseCsv(bytes(header + thousand + "k_1001,TWO,friend\n")) }
    }

    @Test
    @DisplayName("따옴표가 있으면 INVALID_PARAMETER")
    fun quotesRejected() = shouldBeInvalid {
        PoseCsvParser.parsePoseCsv(bytes("media_key,head_count,tag\n\"friend_001\",TWO,friend\n"))
    }

    @Test
    @DisplayName("media.csv - media_key 를 file_name 으로 매핑한다. 다른 열은 무시하고 빈 file_name 은 건너뛴다")
    fun parsesMediaCsv() {
        val mapping = PoseCsvParser.parseMediaCsv(
            bytes(
                "media_key,file_name,width,height,size\r\n" +
                    "friend_001,friend_001.png,1024,1536,2000\r\n" +
                    "friend_002,,1024,1536,2000\r\n" +
                    "friend_001,other.png,1,1,1\r\n",
            ),
        )

        mapping shouldBe mapOf("friend_001" to "friend_001.png")
    }

    @Test
    @DisplayName("media.csv 필수 헤더(file_name) 누락은 INVALID_PARAMETER")
    fun mediaCsvMissingHeader() = shouldBeInvalid {
        PoseCsvParser.parseMediaCsv(bytes("media_key,width\nfriend_001,1024\n"))
    }
}
