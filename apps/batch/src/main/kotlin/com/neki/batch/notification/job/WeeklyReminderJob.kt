package com.neki.batch.notification.job

import com.neki.batch.notification.tasklet.PushNotificationTasklet
import com.neki.batch.notification.tasklet.PushNotificationTaskletFactory
import com.neki.batch.notification.tasklet.WeeklyReminderTargetReader
import com.neki.domain.notification.models.NotificationType
import com.neki.domain.notification.repository.NotificationRepository
import com.neki.domain.photo.repository.PhotoImageRepository
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate

/**
 * fileName       : WeeklyReminderJob
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : WEEKLY_REMINDER 발송 잡. step 하나 = PushNotificationTasklet 하나 (docs/lld/notification-push/weekly-reminder-job.md).
 *
 * RunIdIncrementer: 같은 businessDate 로 다시 돌려도 항상 새 JobInstance 로 처음부터 돈다 (searchIndexJob 과 같은 이유).
 * 중복 발송은 tasklet 의 alreadySent 판정과 notification_log 의 unique 가 막는다 (pipeline.md 5절).
 * tasklet 은 잡 파라미터 businessDate 와 버퍼·커서 상태를 가지므로 @StepScope 다
 */
@Configuration("weeklyReminderJobDefinition") // 기본 빈 이름이 Job 빈(weeklyReminderJob)과 겹쳐 따로 준다
class WeeklyReminderJob(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val tasklets: PushNotificationTaskletFactory,
    private val notificationRepository: NotificationRepository,
    private val photoImageRepository: PhotoImageRepository,
) {

    @Bean(JOB_NAME)
    fun weeklyReminderJob(@Qualifier(STEP_NAME) step: Step): Job = JobBuilder(JOB_NAME, jobRepository)
        .incrementer(RunIdIncrementer())
        .start(step)
        .build()

    /** TaskletStep 은 execute() 마다 트랜잭션을 연다. 커밋 단위 = 대상 1건 */
    @Bean(STEP_NAME)
    fun weeklyReminderStep(@Qualifier(TASKLET_NAME) tasklet: Tasklet): Step = StepBuilder(STEP_NAME, jobRepository)
        .tasklet(tasklet, transactionManager)
        .build()

    @Bean(TASKLET_NAME)
    @StepScope
    fun weeklyReminderTasklet(
        @Value("#{jobParameters['${PushNotificationTasklet.PARAM_BUSINESS_DATE}']}") businessDate: String,
    ): Tasklet {
        val date = LocalDate.parse(businessDate)
        return tasklets.create(
            NotificationType.WEEKLY_REMINDER,
            date,
            WeeklyReminderTargetReader(date, notificationRepository, photoImageRepository),
        )
    }

    companion object {
        const val JOB_NAME = "weeklyReminderJob"
        const val STEP_NAME = "weeklyReminderStep"
        private const val TASKLET_NAME = "weeklyReminderTasklet"
    }
}
