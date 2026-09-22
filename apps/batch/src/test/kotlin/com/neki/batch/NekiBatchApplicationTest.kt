package com.neki.batch

import com.neki.batch.common.job.BatchJobLauncher
import com.neki.batch.sample.SampleJobConfig
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NekiBatchApplicationTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var launcher: BatchJobLauncher

    @Test
    fun `liveness 프로브는 인증 없이 200 을 반환한다`() {
        val response: ResponseEntity<String> =
            restTemplate.getForEntity("/actuator/health/liveness", String::class.java)

        response.statusCode shouldBe HttpStatus.OK
    }

    @Test
    fun `sampleJob 을 기동하면 COMPLETED 로 끝난다`() {
        val execution: JobExecution? = launcher.launchByName(SampleJobConfig.JOB_NAME)

        execution.shouldNotBeNull()
        execution.status shouldBe BatchStatus.COMPLETED
    }

    @Test
    fun `트리거 API 로 sampleJob 을 기동할 수 있다`() {
        val response: ResponseEntity<String> =
            restTemplate.postForEntity("/batch/jobs/sampleJob?businessDate=2026-09-23", null, String::class.java)

        response.statusCode shouldBe HttpStatus.OK
        response.body shouldContain "COMPLETED"
    }

    @Test
    fun `알 수 없는 잡 이름은 404 를 반환한다`() {
        val response: ResponseEntity<String> =
            restTemplate.postForEntity("/batch/jobs/unknownJob", null, String::class.java)

        response.statusCode shouldBe HttpStatus.NOT_FOUND
    }
}
