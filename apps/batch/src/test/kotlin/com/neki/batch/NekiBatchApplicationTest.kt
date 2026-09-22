package com.neki.batch

import com.neki.batch.sample.SampleJobConfig
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class NekiBatchApplicationTest {

    @Autowired
    private lateinit var jobLauncher: JobLauncher

    @Autowired
    private lateinit var jobExplorer: JobExplorer

    @Autowired
    @Qualifier(SampleJobConfig.JOB_NAME)
    private lateinit var sampleJob: Job

    @Test
    fun `sampleJob 은 businessDate 파라미터로 실행되어 COMPLETED 로 끝난다`() {
        val execution: JobExecution = launch("2026-09-23")

        execution.status shouldBe BatchStatus.COMPLETED
        execution.jobParameters.getString("businessDate") shouldBe "2026-09-23"
    }

    @Test
    fun `같은 businessDate 로 다시 기동해도 새 JobInstance 로 돈다`() {
        val first: JobExecution = launch("2026-09-24")
        val second: JobExecution = launch("2026-09-24")

        second.status shouldBe BatchStatus.COMPLETED
        second.jobInstance.instanceId shouldNotBe first.jobInstance.instanceId
    }

    /** 기동 시 JobLauncherApplicationRunner 가 하는 것과 같은 파라미터 구성 (incrementer 적용 뒤 인자 병합) */
    private fun launch(businessDate: String): JobExecution {
        val params: JobParameters = JobParametersBuilder(jobExplorer)
            .getNextJobParameters(sampleJob)
            .addString("businessDate", businessDate)
            .toJobParameters()

        return jobLauncher.run(sampleJob, params)
    }
}
