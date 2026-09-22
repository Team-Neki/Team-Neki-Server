package com.neki.batch.common.job

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobLauncher
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class BatchJobLauncherTest {

    private val jobLauncher: JobLauncher = mockk()
    private val jobExplorer: JobExplorer = mockk()

    // UTC 2026-09-23 15:00 = KST 2026-09-24 00:00. businessDate 가 운영 타임존을 따르는지 확인한다
    private val clock: Clock = Clock.fixed(Instant.parse("2026-09-23T15:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val job: Job = mockk { every { name } returns "sampleJob" }
    private val launcher = BatchJobLauncher(jobLauncher, jobExplorer, clock, listOf(job))

    @Test
    fun `실행 중인 execution 이 있으면 기동하지 않고 null 을 반환한다`() {
        every { jobExplorer.findRunningJobExecutions("sampleJob") } returns setOf(mockk())

        launcher.launch(job).shouldBeNull()

        verify(exactly = 0) { jobLauncher.run(any(), any()) }
    }

    @Test
    fun `운영 타임존 오늘을 businessDate 로, 기동 시각을 launchedAt 으로 넘긴다`() {
        every { jobExplorer.findRunningJobExecutions("sampleJob") } returns emptySet()
        val params = slot<JobParameters>()
        val execution: JobExecution = mockk()
        every { jobLauncher.run(job, capture(params)) } returns execution

        launcher.launch(job) shouldBe execution

        params.captured.getString("businessDate") shouldBe "2026-09-24"
        params.captured.getLong("launchedAt") shouldBe clock.millis()
    }

    @Test
    fun `알 수 없는 잡 이름이면 IllegalArgumentException`() {
        shouldThrow<IllegalArgumentException> { launcher.launchByName("unknownJob") }
    }
}
