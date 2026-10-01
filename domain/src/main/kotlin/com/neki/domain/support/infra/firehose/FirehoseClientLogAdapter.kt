package com.neki.domain.support.infra.firehose

import com.fasterxml.jackson.databind.ObjectMapper
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.support.dto.ClientLogCommand
import com.neki.domain.support.external.ClientLogSender
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.services.firehose.FirehoseClient
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest
import software.amazon.awssdk.services.firehose.model.PutRecordBatchResponse
import software.amazon.awssdk.services.firehose.model.Record
import java.time.Instant

/**
 * fileName       : FirehoseClientLogAdapter
 * author         : koo
 * date           : 2026. 10. 2.
 * description    : 클라이언트 로그를 Kinesis Firehose 로 보내는 어댑터. 로그 1건을 개행으로 끝나는 JSON 한 줄(NDJSON)로 만든다.
 *                  클라이언트 JSON 은 log 필드 아래에 그대로 둬서 서버가 붙이는 userId 등을 덮어쓰지 못하게 한다.
 */
@Profile("!test & !local")
@Component
class FirehoseClientLogAdapter(
    private val firehoseClient: FirehoseClient,
    private val objectMapper: ObjectMapper,
    @Value("\${aws.firehose.delivery-stream}") private val deliveryStream: String,
) : ClientLogSender {

    private val log = LoggerFactory.getLogger(javaClass)

    private companion object {
        // PutRecordBatch 한도. 건수(500)는 요청 검증에서 막는다
        const val MAX_RECORD_BYTES = 1_000 * 1024
        const val MAX_BATCH_BYTES = 4 * 1024 * 1024
    }

    override fun send(command: ClientLogCommand.Collect) {
        val receivedAt: String = Instant.now().toString()
        val lines: List<ByteArray> = command.logs.map { toLine(command, it, receivedAt) }

        // 한도를 넘는 요청은 재시도해도 실패하므로 재시도 대상(LOG_SEND_FAILED)이 아닌 400 으로 끊는다
        if (lines.any { it.size > MAX_RECORD_BYTES } || lines.sumOf { it.size } > MAX_BATCH_BYTES) {
            throw BusinessException(ResultCode.INVALID_PARAMETER)
        }

        val request: PutRecordBatchRequest = PutRecordBatchRequest.builder()
            .deliveryStreamName(deliveryStream)
            .records(lines.map { Record.builder().data(SdkBytes.fromByteArray(it)).build() })
            .build()

        val response: PutRecordBatchResponse = try {
            firehoseClient.putRecordBatch(request)
        } catch (e: SdkException) {
            log.error("Firehose putRecordBatch failed: stream={}, records={}", deliveryStream, lines.size, e)
            throw BusinessException(ResultCode.LOG_SEND_FAILED)
        }

        if (response.failedPutCount() > 0) {
            log.error(
                "Firehose putRecordBatch partially failed: stream={}, failed={}/{}",
                deliveryStream,
                response.failedPutCount(),
                lines.size,
            )
            throw BusinessException(ResultCode.LOG_SEND_FAILED)
        }
    }

    private fun toLine(command: ClientLogCommand.Collect, clientLog: Map<String, Any?>, receivedAt: String): ByteArray {
        val record: Map<String, Any?> = mapOf(
            "userId" to command.userId,
            "platform" to command.platform,
            "appVersion" to command.appVersion,
            "receivedAt" to receivedAt,
            "log" to clientLog,
        )

        return objectMapper.writeValueAsBytes(record) + '\n'.code.toByte()
    }
}
