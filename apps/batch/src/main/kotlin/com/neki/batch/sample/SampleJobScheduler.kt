package com.neki.batch.sample

import com.neki.batch.common.job.BatchJobLauncher
import org.springframework.batch.core.Job
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "neki.batch", name = ["scheduling-enabled"], havingValue = "true")
class SampleJobScheduler(
    private val launcher: BatchJobLauncher,
    @Qualifier(SampleJobConfig.JOB_NAME) private val sampleJob: Job,
) {
    @Scheduled(cron = "\${neki.batch.cron.sample}", zone = "\${neki.batch.zone:Asia/Seoul}")
    fun runSample() {
        launcher.launch(sampleJob)
    }
}
