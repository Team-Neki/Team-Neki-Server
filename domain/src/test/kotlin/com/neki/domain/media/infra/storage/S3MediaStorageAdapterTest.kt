package com.neki.domain.media.infra.storage

import com.neki.config.aws.S3Properties
import com.neki.domain.media.infra.storage.s3.S3MediaStorageAdapter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import java.time.Duration

/**
 * fileName       : S3MediaStorageAdapterTest
 * author         : koo
 * date           : 2026. 10. 02.
 * description    : S3MediaStorageAdapter 의 S3 조회 구간 Timer 기록 테스트
 */
class S3MediaStorageAdapterTest :
    FunSpec({

        lateinit var s3Client: S3Client
        lateinit var meterRegistry: SimpleMeterRegistry
        lateinit var adapter: S3MediaStorageAdapter

        beforeTest {
            s3Client = mockk()
            meterRegistry = SimpleMeterRegistry()
            adapter = S3MediaStorageAdapter(
                s3Client = s3Client,
                s3Presigner = mockk(),
                props = S3Properties(
                    accessKey = "access",
                    secretKey = "secret",
                    region = "ap-northeast-2",
                    bucket = "bucket",
                    presignedUrlExpiration = Duration.ofMinutes(5),
                ),
                meterRegistry = meterRegistry,
            )
        }

        test("S3 조회 시간을 미디어 타입 태그로 기록한다") {
            val data = byteArrayOf(1, 2, 3)
            every { s3Client.getObjectAsBytes(any<GetObjectRequest>()) } returns
                ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), data)

            adapter.fetchBinaryByKey("photo-booth/1/a.jpg") shouldBe data

            val timer: Timer = meterRegistry.get("media.storage.fetch").tag("type", "photo-booth").timer()
            timer.count() shouldBe 1
        }

        test("실패한 조회는 기록하지 않는다") {
            every { s3Client.getObjectAsBytes(any<GetObjectRequest>()) } throws
                NoSuchKeyException.builder().message("missing").build()

            shouldThrow<NoSuchKeyException> { adapter.fetchBinaryByKey("photo-booth/1/missing.jpg") }

            meterRegistry.find("media.storage.fetch").timer().shouldBeNull()
        }
    })
