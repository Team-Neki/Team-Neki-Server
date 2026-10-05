package com.neki.batch.notification.tasklet

import com.neki.batch.notification.job.NotificationPushJobConfig
import com.neki.domain.notification.external.PushNotificationSender
import com.neki.domain.notification.models.NotificationType
import com.neki.domain.notification.repository.HolidayRepository
import com.neki.domain.notification.repository.NotificationLogRepository
import com.neki.domain.notification.repository.NotificationRepository
import com.neki.domain.notification.service.NotificationService
import com.neki.domain.photo.repository.PhotoImageRepository
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepScope
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
 * fileName       : NotificationPushStepConfig
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : 알림 발송 step 셋. step 마다 tasklet 하나이고, 잡마다 다른 것은 NotificationType 과 SendTargetReader 뿐이다.
 *                  tasklet 은 잡 파라미터 businessDate 와 버퍼·커서 상태를 가지므로 @StepScope 다 (실행마다 새 인스턴스)
 */
@Configuration
class NotificationPushStepConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val notificationRepository: NotificationRepository,
    private val photoImageRepository: PhotoImageRepository,
    private val holidayRepository: HolidayRepository,
    private val notificationLogRepository: NotificationLogRepository,
    private val pushNotificationSender: PushNotificationSender,
    private val notificationService: NotificationService,
) {

    @Bean(WEEKLY_REMINDER_STEP)
    fun weeklyReminderStep(@Qualifier(WEEKLY_REMINDER_TASKLET) tasklet: Tasklet): Step =
        step(WEEKLY_REMINDER_STEP, tasklet)

    @Bean(WEEKLY_REMINDER_TASKLET)
    @StepScope
    fun weeklyReminderTasklet(
        @Value("#{jobParameters['${NotificationPushJobConfig.PARAM_BUSINESS_DATE}']}") businessDate: String,
    ): Tasklet {
        val date = LocalDate.parse(businessDate)
        return tasklet(
            NotificationType.WEEKLY_REMINDER,
            date,
            WeeklyReminderTargetReader(date, notificationRepository, photoImageRepository),
        )
    }

    @Bean(WEEKEND_EXPLORE_STEP)
    fun weekendExploreStep(@Qualifier(WEEKEND_EXPLORE_TASKLET) tasklet: Tasklet): Step =
        step(WEEKEND_EXPLORE_STEP, tasklet)

    @Bean(WEEKEND_EXPLORE_TASKLET)
    @StepScope
    fun weekendExploreTasklet(
        @Value("#{jobParameters['${NotificationPushJobConfig.PARAM_BUSINESS_DATE}']}") businessDate: String,
    ): Tasklet = tasklet(
        NotificationType.WEEKEND_EXPLORE,
        LocalDate.parse(businessDate),
        WeekendExploreTargetReader(notificationRepository),
    )

    @Bean(HOLIDAY_EXPLORE_STEP)
    fun holidayExploreStep(@Qualifier(HOLIDAY_EXPLORE_TASKLET) tasklet: Tasklet): Step =
        step(HOLIDAY_EXPLORE_STEP, tasklet)

    @Bean(HOLIDAY_EXPLORE_TASKLET)
    @StepScope
    fun holidayExploreTasklet(
        @Value("#{jobParameters['${NotificationPushJobConfig.PARAM_BUSINESS_DATE}']}") businessDate: String,
    ): Tasklet {
        val date = LocalDate.parse(businessDate)
        return tasklet(
            NotificationType.HOLIDAY_EXPLORE,
            date,
            HolidayExploreTargetReader(
                date,
                holidayRepository.findByNotifyDate(date),
                notificationRepository,
                photoImageRepository,
            ),
        )
    }

    /** TaskletStep 은 execute() 마다 트랜잭션을 연다. 커밋 단위 = 대상 1건 (PushNotificationTasklet) */
    private fun step(name: String, tasklet: Tasklet): Step =
        StepBuilder(name, jobRepository).tasklet(tasklet, transactionManager).build()

    private fun tasklet(type: NotificationType, businessDate: LocalDate, reader: SendTargetReader): Tasklet =
        PushNotificationTasklet(
            type,
            businessDate,
            reader,
            PAGE_SIZE,
            notificationLogRepository,
            pushNotificationSender,
            notificationService,
        )

    companion object {
        const val WEEKLY_REMINDER_STEP = "weeklyReminderStep"
        const val WEEKEND_EXPLORE_STEP = "weekendExploreStep"
        const val HOLIDAY_EXPLORE_STEP = "holidayExploreStep"

        /** 동의자 DB 조회 단위. 커밋 단위(1건)와 다르다 */
        const val PAGE_SIZE = 100

        private const val WEEKLY_REMINDER_TASKLET = "weeklyReminderTasklet"
        private const val WEEKEND_EXPLORE_TASKLET = "weekendExploreTasklet"
        private const val HOLIDAY_EXPLORE_TASKLET = "holidayExploreTasklet"
    }
}
