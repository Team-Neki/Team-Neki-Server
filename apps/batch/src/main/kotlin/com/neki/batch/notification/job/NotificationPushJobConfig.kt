package com.neki.batch.notification.job

import com.neki.batch.notification.holiday.HolidayCalendar
import com.neki.batch.notification.step.HolidayExploreTargetReader
import com.neki.batch.notification.step.NotificationItemProcessor
import com.neki.batch.notification.step.NotificationItemWriter
import com.neki.batch.notification.step.PagingSendTargetItemReader
import com.neki.batch.notification.step.PreparedNotification
import com.neki.batch.notification.step.WeekendExploreTargetReader
import com.neki.batch.notification.step.WeeklyReminderTargetReader
import com.neki.domain.notification.models.NotificationType
import com.neki.domain.notification.models.SendTarget
import com.neki.domain.notification.repository.NotificationLogRepository
import com.neki.domain.notification.repository.NotificationRepository
import com.neki.domain.photo.repository.PhotoImageRepository
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate

/**
 * fileName       : NotificationPushJobConfig
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : 알림 발송 잡 3개. 종류마다 Reader 와 Processor 가 @StepScope 빈이고, Step·Job 조립은 pushJob 하나가 한다.
 *                  RunIdIncrementer: 같은 businessDate 로 다시 돌려도 새 JobInstance. 중복 발송은 Processor 의 alreadySent 와
 *                  notification_log 의 unique 가 막는다. 공통 규칙은 docs/lld/notification-push/pipeline.md
 */
@Configuration
class NotificationPushJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val notificationLogRepository: NotificationLogRepository,
    private val writer: NotificationItemWriter,
) {

    @Bean(WEEKLY_REMINDER_READER)
    @StepScope
    fun weeklyReminderItemReader(
        @Value("#{jobParameters['$PARAM_BUSINESS_DATE']}") businessDate: String,
        notificationRepository: NotificationRepository,
        photoImageRepository: PhotoImageRepository,
    ): ItemReader<SendTarget> = PagingSendTargetItemReader(
        WeeklyReminderTargetReader(LocalDate.parse(businessDate), notificationRepository, photoImageRepository),
        PAGE_SIZE,
    )

    @Bean(WEEKLY_REMINDER_PROCESSOR)
    @StepScope
    fun weeklyReminderItemProcessor(
        @Value("#{jobParameters['$PARAM_BUSINESS_DATE']}") businessDate: String,
    ): ItemProcessor<SendTarget, PreparedNotification> = processor(NotificationType.WEEKLY_REMINDER, businessDate)

    @Bean(WEEKLY_REMINDER_JOB)
    fun weeklyReminderJob(
        @Qualifier(WEEKLY_REMINDER_READER) reader: ItemReader<SendTarget>,
        @Qualifier(WEEKLY_REMINDER_PROCESSOR) processor: ItemProcessor<SendTarget, PreparedNotification>,
    ): Job = pushJob(WEEKLY_REMINDER_JOB, reader, processor)

    @Bean(WEEKEND_EXPLORE_READER)
    @StepScope
    fun weekendExploreItemReader(notificationRepository: NotificationRepository): ItemReader<SendTarget> =
        PagingSendTargetItemReader(WeekendExploreTargetReader(notificationRepository), PAGE_SIZE)

    @Bean(WEEKEND_EXPLORE_PROCESSOR)
    @StepScope
    fun weekendExploreItemProcessor(
        @Value("#{jobParameters['$PARAM_BUSINESS_DATE']}") businessDate: String,
    ): ItemProcessor<SendTarget, PreparedNotification> = processor(NotificationType.WEEKEND_EXPLORE, businessDate)

    @Bean(WEEKEND_EXPLORE_JOB)
    fun weekendExploreJob(
        @Qualifier(WEEKEND_EXPLORE_READER) reader: ItemReader<SendTarget>,
        @Qualifier(WEEKEND_EXPLORE_PROCESSOR) processor: ItemProcessor<SendTarget, PreparedNotification>,
    ): Job = pushJob(WEEKEND_EXPLORE_JOB, reader, processor)

    @Bean(HOLIDAY_EXPLORE_READER)
    @StepScope
    fun holidayExploreItemReader(
        @Value("#{jobParameters['$PARAM_BUSINESS_DATE']}") businessDate: String,
        holidayCalendar: HolidayCalendar,
        notificationRepository: NotificationRepository,
        photoImageRepository: PhotoImageRepository,
    ): ItemReader<SendTarget> {
        val date = LocalDate.parse(businessDate)
        return PagingSendTargetItemReader(
            HolidayExploreTargetReader(
                date,
                holidayCalendar.holidayOn(date),
                notificationRepository,
                photoImageRepository,
            ),
            PAGE_SIZE,
        )
    }

    @Bean(HOLIDAY_EXPLORE_PROCESSOR)
    @StepScope
    fun holidayExploreItemProcessor(
        @Value("#{jobParameters['$PARAM_BUSINESS_DATE']}") businessDate: String,
    ): ItemProcessor<SendTarget, PreparedNotification> = processor(NotificationType.HOLIDAY_EXPLORE, businessDate)

    @Bean(HOLIDAY_EXPLORE_JOB)
    fun holidayExploreJob(
        @Qualifier(HOLIDAY_EXPLORE_READER) reader: ItemReader<SendTarget>,
        @Qualifier(HOLIDAY_EXPLORE_PROCESSOR) processor: ItemProcessor<SendTarget, PreparedNotification>,
    ): Job = pushJob(HOLIDAY_EXPLORE_JOB, reader, processor)

    private fun processor(
        type: NotificationType,
        businessDate: String,
    ): ItemProcessor<SendTarget, PreparedNotification> =
        NotificationItemProcessor(type, LocalDate.parse(businessDate), notificationLogRepository)

    /** 청크 1건 = 트랜잭션 1건. Step 이름은 잡 이름의 Job 을 Step 으로 바꾼 것 (weeklyReminderStep) */
    private fun pushJob(
        name: String,
        reader: ItemReader<SendTarget>,
        processor: ItemProcessor<SendTarget, PreparedNotification>,
    ): Job {
        val step: Step = StepBuilder("${name.removeSuffix("Job")}Step", jobRepository)
            .chunk<SendTarget, PreparedNotification>(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build()
        return JobBuilder(name, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(step)
            .build()
    }

    companion object {
        const val WEEKLY_REMINDER_JOB = "weeklyReminderJob"
        const val WEEKEND_EXPLORE_JOB = "weekendExploreJob"
        const val HOLIDAY_EXPLORE_JOB = "holidayExploreJob"

        /** Prefect 가 넘기는 잡 파라미터. 발송의 논리적 날짜 (businessDate=2026-10-06) */
        const val PARAM_BUSINESS_DATE = "businessDate"

        /** 발송(외부 부수효과)과 적재가 dual-write 라 1건씩 커밋한다 (pipeline.md 1절) */
        const val CHUNK_SIZE = 1
        const val PAGE_SIZE = 100

        private const val WEEKLY_REMINDER_READER = "weeklyReminderItemReader"
        private const val WEEKLY_REMINDER_PROCESSOR = "weeklyReminderItemProcessor"
        private const val WEEKEND_EXPLORE_READER = "weekendExploreItemReader"
        private const val WEEKEND_EXPLORE_PROCESSOR = "weekendExploreItemProcessor"
        private const val HOLIDAY_EXPLORE_READER = "holidayExploreItemReader"
        private const val HOLIDAY_EXPLORE_PROCESSOR = "holidayExploreItemProcessor"
    }
}
