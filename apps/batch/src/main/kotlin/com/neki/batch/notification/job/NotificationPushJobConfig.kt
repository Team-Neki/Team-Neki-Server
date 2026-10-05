package com.neki.batch.notification.job

import com.neki.batch.notification.tasklet.NotificationPushStepConfig
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * fileName       : NotificationPushJobConfig
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : 알림 발송 잡 3개. 종류마다 Job 하나, step 하나 (tasklet/NotificationPushStepConfig).
 *
 * RunIdIncrementer: 같은 businessDate 로 다시 돌려도 항상 새 JobInstance 로 처음부터 돈다 (searchIndexJob 과 같은 이유).
 * 중복 발송은 tasklet 의 alreadySent 판정과 notification_log 의 unique 가 막는다. 공통 규칙은 docs/lld/notification-push/pipeline.md
 */
@Configuration
class NotificationPushJobConfig {

    @Bean(WEEKLY_REMINDER_JOB)
    fun weeklyReminderJob(
        jobRepository: JobRepository,
        @Qualifier(NotificationPushStepConfig.WEEKLY_REMINDER_STEP) weeklyReminderStep: Step,
    ): Job = job(WEEKLY_REMINDER_JOB, jobRepository, weeklyReminderStep)

    @Bean(WEEKEND_EXPLORE_JOB)
    fun weekendExploreJob(
        jobRepository: JobRepository,
        @Qualifier(NotificationPushStepConfig.WEEKEND_EXPLORE_STEP) weekendExploreStep: Step,
    ): Job = job(WEEKEND_EXPLORE_JOB, jobRepository, weekendExploreStep)

    @Bean(HOLIDAY_EXPLORE_JOB)
    fun holidayExploreJob(
        jobRepository: JobRepository,
        @Qualifier(NotificationPushStepConfig.HOLIDAY_EXPLORE_STEP) holidayExploreStep: Step,
    ): Job = job(HOLIDAY_EXPLORE_JOB, jobRepository, holidayExploreStep)

    private fun job(name: String, jobRepository: JobRepository, step: Step): Job = JobBuilder(name, jobRepository)
        .incrementer(RunIdIncrementer())
        .start(step)
        .build()

    companion object {
        const val WEEKLY_REMINDER_JOB = "weeklyReminderJob"
        const val WEEKEND_EXPLORE_JOB = "weekendExploreJob"
        const val HOLIDAY_EXPLORE_JOB = "holidayExploreJob"

        /** Prefect 가 넘기는 잡 파라미터. 발송의 논리적 날짜 (businessDate=2026-10-06) */
        const val PARAM_BUSINESS_DATE = "businessDate"
    }
}
