package com.neki.api.support.infra.firehose

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.support.dto.ClientLogCommand
import com.neki.domain.support.infra.firehose.FirehoseClientLogAdapter
import com.neki.domain.support.models.Platform
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import software.amazon.awssdk.services.firehose.FirehoseClient
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest
import software.amazon.awssdk.services.firehose.model.PutRecordBatchResponse

@DisplayName("FirehoseClientLogAdapter")
class FirehoseClientLogAdapterTest {

    private val firehoseClient: FirehoseClient = mockk()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val adapter = FirehoseClientLogAdapter(firehoseClient, objectMapper, "team-neki-log-raw-test")

    private fun command(vararg logs: Map<String, Any?>) =
        ClientLogCommand.Collect(userId = 7, platform = Platform.ANDROID, logs = logs.toList())

    @Test
    @DisplayName("로그마다 서버 필드와 원본 log 를 담은 NDJSON 줄을 만들어 레코드 하나에 이어 붙인다")
    fun packsNdjsonLinesIntoOneRecord() {
        val request = slot<PutRecordBatchRequest>()
        every { firehoseClient.putRecordBatch(capture(request)) } returns
            PutRecordBatchResponse.builder().failedPutCount(0).build()

        adapter.send(
            command(
                mapOf("userId" to 999, "appVersion" to "1.4.0", "message" to "boom"),
                mapOf("event" to "tap"),
            ),
        )

        assertEquals("team-neki-log-raw-test", request.captured.deliveryStreamName())
        assertEquals(1, request.captured.records().size)
        val data: String = request.captured.records().single().data().asUtf8String()
        assertTrue(data.endsWith("\n"))
        val lines: List<String> = data.removeSuffix("\n").split("\n")
        assertEquals(2, lines.size)

        val first: Map<*, *> = objectMapper.readValue(lines[0], Map::class.java)
        assertEquals(7, first["userId"]) // 클라이언트가 보낸 userId 는 log 아래에 남고 덮어쓰지 못한다
        assertEquals("ANDROID", first["platform"])
        assertEquals(setOf("userId", "platform", "receivedAt", "log"), first.keys)
        assertEquals(mapOf("userId" to 999, "appVersion" to "1.4.0", "message" to "boom"), first["log"])
        assertTrue(first["receivedAt"] is String)
        assertEquals(mapOf("event" to "tap"), objectMapper.readValue(lines[1], Map::class.java)["log"])
    }

    @Test
    @DisplayName("레코드 한도(1,000KiB)를 넘으면 줄 순서를 지키며 레코드를 나눈다")
    fun splitsRecordsAtLimitKeepingOrder() {
        val request = slot<PutRecordBatchRequest>()
        every { firehoseClient.putRecordBatch(capture(request)) } returns
            PutRecordBatchResponse.builder().failedPutCount(0).build()
        val big = "x".repeat(400 * 1024)

        adapter.send(
            command(mapOf("seq" to 1, "m" to big), mapOf("seq" to 2, "m" to big), mapOf("seq" to 3, "m" to big)),
        )

        val records: List<ByteArray> = request.captured.records().map { it.data().asByteArray() }
        assertEquals(2, records.size)
        assertTrue(records.all { it.size <= 1_000 * 1024 })
        val seqs: List<Any?> = records.flatMap { it.decodeToString().removeSuffix("\n").split("\n") }
            .map { (objectMapper.readValue(it, Map::class.java)["log"] as Map<*, *>)["seq"] }
        assertEquals(listOf(1, 2, 3), seqs)
    }

    @Test
    @DisplayName("일부 레코드가 실패하면 재시도 대상인 LOG_SEND_FAILED 를 던진다")
    fun partialFailureThrowsRetryable() {
        every { firehoseClient.putRecordBatch(any<PutRecordBatchRequest>()) } returns
            PutRecordBatchResponse.builder().failedPutCount(1).build()

        val ex = assertThrows<BusinessException> { adapter.send(command(mapOf("a" to 1), mapOf("b" to 2))) }

        assertEquals(ResultCode.LOG_SEND_FAILED, ex.resultCode)
    }

    @Test
    @DisplayName("Firehose 레코드 한도(1,000KiB)를 넘는 로그는 보내지 않고 INVALID_PARAMETER 를 던진다")
    fun oversizedRecordIsRejectedWithoutCall() {
        val ex = assertThrows<BusinessException> { adapter.send(command(mapOf("m" to "x".repeat(1_000 * 1024)))) }

        assertEquals(ResultCode.INVALID_PARAMETER, ex.resultCode)
        verify(exactly = 0) { firehoseClient.putRecordBatch(any<PutRecordBatchRequest>()) }
    }
}
