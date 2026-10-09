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
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.concurrent.Semaphore

/**
 * fileName       : FirehoseClientLogAdapter
 * author         : koo
 * date           : 2026. 10. 2.
 * description    : 클라이언트 로그를 Kinesis Firehose 로 보내는 어댑터. 로그 1건을 개행으로 끝나는 JSON 한 줄(NDJSON)로 만들고,
 *                  줄들을 레코드 한도까지 이어 붙여 보낸다. Firehose 는 레코드마다 5KB 로 올려 과금하므로
 *                  로그 1건 = 레코드 1건이면 수백 바이트 로그도 5KB 값을 낸다 (Platform ADR-0004).
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
        // PutRecordBatch 한도. 레코드 수는 로그 수(최대 500, 요청 검증) 이하라 따로 보지 않는다
        const val MAX_RECORD_BYTES = 1_000 * 1024
        const val MAX_BATCH_BYTES = 4 * 1024 * 1024

        // 로그 전송이 동시에 붙잡을 수 있는 Tomcat 스레드 수 (pod 당). Firehose 가 느려져 전송마다 타임아웃(5초)까지
        // 걸려도 기본 200개 중 나머지는 다른 API 몫으로 남긴다
        const val MAX_IN_FLIGHT = 20
    }

    private val inFlight = Semaphore(MAX_IN_FLIGHT)

    override fun send(command: ClientLogCommand.Collect) {
        val receivedAt: String = Instant.now().toString()
        val lines: List<ByteArray> = command.logs.map { toLine(command, it, receivedAt) }

        // 한도를 넘는 요청은 재시도해도 실패하므로 재시도 대상(LOG_SEND_FAILED)이 아닌 400 으로 끊는다
        if (lines.any { it.size > MAX_RECORD_BYTES } || lines.sumOf { it.size } > MAX_BATCH_BYTES) {
            throw BusinessException(ResultCode.INVALID_PARAMETER)
        }

        val request: PutRecordBatchRequest = PutRecordBatchRequest.builder()
            .deliveryStreamName(deliveryStream)
            .records(pack(lines).map { Record.builder().data(SdkBytes.fromByteArray(it)).build() })
            .build()

        // 자리가 없으면 기다리지 않고 재시도 대상으로 끊는다. 클라이언트가 배치를 들고 있다가 나중에 다시 보낸다
        if (!inFlight.tryAcquire()) {
            log.warn("Firehose putRecordBatch skipped: stream={}, inFlight={}", deliveryStream, MAX_IN_FLIGHT)
            throw BusinessException(ResultCode.LOG_SEND_FAILED)
        }

        val response: PutRecordBatchResponse = try {
            firehoseClient.putRecordBatch(request)
        } catch (e: SdkException) {
            log.error("Firehose putRecordBatch failed: stream={}, logs={}", deliveryStream, lines.size, e)
            throw BusinessException(ResultCode.LOG_SEND_FAILED)
        } finally {
            inFlight.release()
        }

        if (response.failedPutCount() > 0) {
            log.error(
                "Firehose putRecordBatch partially failed: stream={}, failedRecords={}/{}",
                deliveryStream,
                response.failedPutCount(),
                request.records().size,
            )
            throw BusinessException(ResultCode.LOG_SEND_FAILED)
        }
    }

    /** 줄 순서를 지키며 레코드 한도까지 이어 붙인다. 줄 하나는 한도 이하임이 보장된 상태로 들어온다. */
    private fun pack(lines: List<ByteArray>): List<ByteArray> {
        val records = mutableListOf<ByteArray>()
        val current = ByteArrayOutputStream()

        for (line in lines) {
            if (current.size() + line.size > MAX_RECORD_BYTES) {
                records += current.toByteArray()
                current.reset()
            }
            current.write(line)
        }
        records += current.toByteArray()

        return records
    }

    private fun toLine(command: ClientLogCommand.Collect, clientLog: Map<String, Any?>, receivedAt: String): ByteArray {
        val record: Map<String, Any?> = mapOf(
            "userId" to command.userId,
            "platform" to command.platform,
            "receivedAt" to receivedAt,
            "log" to clientLog,
        )

        return objectMapper.writeValueAsBytes(record) + '\n'.code.toByte()
    }
}
