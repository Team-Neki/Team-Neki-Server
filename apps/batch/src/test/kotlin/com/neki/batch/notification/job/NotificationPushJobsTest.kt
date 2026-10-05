package com.neki.batch.notification.job

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.notification.external.PushNotificationSender
import com.neki.domain.notification.infra.persist.jpa.JpaNotificationHistRepository
import com.neki.domain.notification.infra.persist.jpa.JpaNotificationLogRepository
import com.neki.domain.notification.infra.persist.jpa.JpaNotificationRepository
import com.neki.domain.notification.models.FcmSendStatus
import com.neki.domain.notification.models.Notification
import com.neki.domain.notification.models.NotificationLog
import com.neki.domain.notification.models.NotificationType
import com.neki.domain.photo.infra.persist.jpa.JpaPhotoImageRepository
import com.neki.domain.photo.models.PhotoImage
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
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
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.sql.Timestamp
import java.util.concurrent.atomic.AtomicLong

/**
 * 잡 3종을 H2 에서 끝까지 돌린다. FCM 은 RecordingPushSender 로 바꿔 호출 토큰을 기록한다.
 * businessDate 2026-06-18(목) 은 epochDay 20622 가 3 의 배수라 톤이 userId % 3 과 같다.
 * created_at 은 auditing 이 덮어쓰므로 저장 뒤 SQL 로 바꾼다
 */
@SpringBootTest
@ActiveProfiles("test")
class NotificationPushJobsTest {

    @TestConfiguration
    class StubConfig {
        @Bean
        @Primary
        fun recordingPushSender(): RecordingPushSender = RecordingPushSender()
    }

    class RecordingPushSender : PushNotificationSender {
        val sent = mutableListOf<String>()
        var failingTokens: Set<String> = emptySet()
        var configured: Boolean = true

        override fun send(token: String, title: String, body: String, link: String?): String {
            if (!configured) throw BusinessException(ResultCode.PUSH_NOT_CONFIGURED)
            if (token in failingTokens) throw BusinessException(ResultCode.PUSH_SEND_FAILED)
            sent += token
            return "message-${sent.size}"
        }

        fun reset() {
            sent.clear()
            failingTokens = emptySet()
            configured = true
        }
    }

    @Autowired
    private lateinit var jobLauncher: JobLauncher

    @Autowired
    private lateinit var jobExplorer: JobExplorer

    @Autowired
    @Qualifier(NotificationPushJobConfig.WEEKLY_REMINDER_JOB)
    private lateinit var weeklyReminderJob: Job

    @Autowired
    @Qualifier(NotificationPushJobConfig.WEEKEND_EXPLORE_JOB)
    private lateinit var weekendExploreJob: Job

    @Autowired
    @Qualifier(NotificationPushJobConfig.HOLIDAY_EXPLORE_JOB)
    private lateinit var holidayExploreJob: Job

    @Autowired
    private lateinit var notificationRepository: JpaNotificationRepository

    @Autowired
    private lateinit var photoImageRepository: JpaPhotoImageRepository

    @Autowired
    private lateinit var logRepository: JpaNotificationLogRepository

    @Autowired
    private lateinit var histRepository: JpaNotificationHistRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var pushSender: RecordingPushSender

    @AfterEach
    fun tearDown() {
        pushSender.reset()
        logRepository.deleteAllInBatch()
        histRepository.deleteAllInBatch()
        // deleteAllInBatch 는 @SQLRestriction 때문에 soft-delete 행을 못 지운다
        jdbcTemplate.update("delete from tb_photo_image")
        notificationRepository.deleteAllInBatch()
    }

    @Test
    fun `weekend - 동의자 전원에게 발송하고 log 와 hist 를 적재한다, 미동의는 제외`() {
        givenUsers()

        launch(weekendExploreJob).status shouldBe BatchStatus.COMPLETED

        logUserIds(NotificationType.WEEKEND_EXPLORE) shouldContainExactly listOf(1L, 3L, 4L, 5L)
        logRepository.findAll().map { it.fcmResult }.toSet() shouldBe setOf(FcmSendStatus.SUCCESS)
        pushSender.sent shouldContainExactly listOf("tok-1", "tok-3", "tok-4", "tok-5")
        histUserIds("WEEKEND_EXPLORE") shouldContainExactly listOf(1L, 3L, 4L, 5L)
    }

    @Test
    fun `weekend - 같은 businessDate 로 다시 돌리면 발송 0건이고 새 JobInstance 다`() {
        givenUsers()
        val first: JobExecution = launch(weekendExploreJob)
        pushSender.sent.clear()

        val second: JobExecution = launch(weekendExploreJob)

        second.status shouldBe BatchStatus.COMPLETED
        second.jobInstance.instanceId shouldNotBe first.jobInstance.instanceId
        pushSender.sent.shouldBeEmpty()
        logRepository.count() shouldBe 4
        histRepository.count() shouldBe 4
    }

    @Test
    fun `weekly - D-7 업로드 동의자만, SUGGESTIVE 톤은 요일을 치환한다`() {
        givenUsers()

        launch(weeklyReminderJob).status shouldBe BatchStatus.COMPLETED

        logUserIds(NotificationType.WEEKLY_REMINDER) shouldContainExactly listOf(1L, 5L)
        val user5: NotificationLog = logRepository.findAll().single { it.userId == 5L }
        user5.title shouldBe "지난 목요일처럼 오늘도 남겨볼까요?"
        user5.variableApplied shouldBe true
        val user1: NotificationLog = logRepository.findAll().single { it.userId == 1L }
        user1.title shouldBe "벌써 일주일 전 네컷이에요"
        user1.variableApplied shouldBe false
        histRepository.findAll().single { it.userId == 5L }.title shouldBe "지난 목요일처럼 오늘도 남겨볼까요?"
    }

    @Test
    fun `weekly - soft-delete 된 사진은 업로드 집계에서 빠진다`() {
        givenUsers()
        jdbcTemplate.update(
            "update tb_photo_image set deleted_at = ? where user_id = 1 and created_at = ?",
            Timestamp.valueOf("2026-06-12 00:00:00"),
            Timestamp.valueOf("2026-06-11 10:00:00"),
        )

        launch(weeklyReminderJob).status shouldBe BatchStatus.COMPLETED

        logUserIds(NotificationType.WEEKLY_REMINDER) shouldContainExactly listOf(5L)
    }

    @Test
    fun `weekly - 첫 페이지가 통째로 걸러져도 다음 페이지의 대상이 발송된다`() {
        (101L..250L).forEach { notification(it, agreed = true) }
        upload(250L, "2026-06-11 10:00:00")

        val execution: JobExecution = launch(weeklyReminderJob)

        execution.status shouldBe BatchStatus.COMPLETED
        logUserIds(NotificationType.WEEKLY_REMINDER) shouldContainExactly listOf(250L)
        execution.stepExecutions.single().readCount shouldBe 1
    }

    @Test
    fun `holiday - 발송일이 아니면 아무것도 보내지 않는다`() {
        givenUsers()

        launch(holidayExploreJob, businessDate = "2026-06-17").status shouldBe BatchStatus.COMPLETED

        pushSender.sent.shouldBeEmpty()
        logRepository.count() shouldBe 0
    }

    @Test
    fun `holiday - 발송일이면 최근 1달 업로드 동의자에게 공휴일명을 치환해 보낸다`() {
        givenUsers()

        launch(holidayExploreJob).status shouldBe BatchStatus.COMPLETED

        logUserIds(NotificationType.HOLIDAY_EXPLORE) shouldContainExactly listOf(1L, 4L, 5L)
        logRepository.findAll().single { it.userId == 1L }.title shouldBe "테스트공휴일에 약속 있으신가요?"
    }

    @Test
    fun `발송 실패 건은 FAILED 로 남기고 hist 없이 나머지를 계속 보낸다`() {
        givenUsers()
        pushSender.failingTokens = setOf("tok-3")

        launch(weekendExploreJob).status shouldBe BatchStatus.COMPLETED

        logRepository.findAll().single { it.userId == 3L }.fcmResult shouldBe FcmSendStatus.FAILED
        pushSender.sent shouldContainExactly listOf("tok-1", "tok-4", "tok-5")
        histUserIds("WEEKEND_EXPLORE") shouldContainExactly listOf(1L, 4L, 5L)
    }

    @Test
    fun `FCM 이 설정되지 않았으면 잡이 실패하고 아무것도 적재하지 않는다`() {
        givenUsers()
        pushSender.configured = false

        launch(weekendExploreJob).status shouldBe BatchStatus.FAILED

        logRepository.count() shouldBe 0
    }

    /** 기동 시 JobLauncherApplicationRunner 와 같은 파라미터 구성 (incrementer 뒤 인자 병합). 명령줄 인자는 문자열 */
    private fun launch(job: Job, businessDate: String = BUSINESS_DATE): JobExecution {
        val params: JobParameters = JobParametersBuilder(jobExplorer)
            .getNextJobParameters(job)
            .addString(NotificationPushJobConfig.PARAM_BUSINESS_DATE, businessDate)
            .toJobParameters()
        return jobLauncher.run(job, params)
    }

    /** 유저 1~5. 2 는 미동의. 업로드: 1(06-11, 06-17), 2(06-11), 4(05-29), 5(06-11) */
    private fun givenUsers() {
        notification(1L, agreed = true)
        notification(2L, agreed = false)
        notification(3L, agreed = true)
        notification(4L, agreed = true)
        notification(5L, agreed = true)
        upload(1L, "2026-06-11 10:00:00")
        upload(1L, "2026-06-17 09:00:00")
        upload(2L, "2026-06-11 10:00:00")
        upload(4L, "2026-05-29 12:00:00")
        upload(5L, "2026-06-11 10:00:00")
    }

    private fun notification(userId: Long, agreed: Boolean) {
        notificationRepository.save(Notification(userId = userId, deviceToken = "tok-$userId", pushAgreed = agreed))
    }

    private fun upload(userId: Long, createdAt: String) {
        val saved: PhotoImage = photoImageRepository.save(
            PhotoImage(userId = userId, mediaId = mediaSeq.incrementAndGet()),
        )
        jdbcTemplate.update(
            "update tb_photo_image set created_at = ? where id = ?",
            Timestamp.valueOf(createdAt),
            saved.id,
        )
    }

    private fun logUserIds(type: NotificationType): List<Long> =
        logRepository.findAll().filter { it.notificationType == type }.map { it.userId }.sorted()

    private fun histUserIds(type: String): List<Long> =
        histRepository.findAll().filter { it.type == type }.map { it.userId }.sorted()

    companion object {
        private const val BUSINESS_DATE = "2026-06-18"
        private val mediaSeq = AtomicLong(1_000L)
    }
}
