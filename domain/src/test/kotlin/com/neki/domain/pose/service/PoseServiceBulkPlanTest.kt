package com.neki.domain.pose.service

import com.neki.domain.pose.dto.PoseCommand
import com.neki.domain.pose.models.BulkFailReason
import com.neki.domain.pose.models.BulkRowOutcome
import com.neki.domain.pose.models.BulkSkipReason
import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.PoseBulkUploadPlan
import com.neki.domain.pose.models.PoseImageFile
import com.neki.domain.pose.models.PoseRelation
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.text.Normalizer

class PoseServiceBulkPlanTest {

    private val service = PoseService(mockk())

    private fun row(
        line: Int,
        key: String,
        headCount: String = "TWO",
        tag: String? = "friend",
        fileName: String? = null,
    ) = PoseCommand.BulkUploadPoses.Row(line, key, headCount, tag, fileName)

    private fun image(name: String) = PoseImageFile(name) { ByteArray(0) }

    private fun plan(
        rows: List<PoseCommand.BulkUploadPoses.Row>,
        images: List<String> = emptyList(),
        existing: Set<String> = emptySet(),
    ): PoseBulkUploadPlan =
        service.planBulkUpload(PoseCommand.BulkUploadPoses(1L, rows, images.map { image(it) }), existing)

    @Test
    @DisplayName("정상 행은 relation 과 headCount 를 해석해 toCreate 에 CSV 순서대로 담는다")
    fun createsInCsvOrder() {
        val result = plan(
            rows = listOf(row(2, "friend_001"), row(3, "ssum_001", tag = "SSUM"), row(4, "solo_001", "one", "")),
            images = listOf("solo_001.png", "friend_001.png", "ssum_001.jpg"),
        )

        result.toCreate.map { Triple(it.sourceKey, it.headCount, it.relation) } shouldBe listOf(
            Triple("friend_001", HeadCount.TWO, PoseRelation.FRIEND),
            Triple("ssum_001", HeadCount.TWO, PoseRelation.SSUM),
            Triple("solo_001", HeadCount.ONE, null),
        )
        result.toCreate.map { it.image.fileName } shouldBe listOf("friend_001.png", "ssum_001.jpg", "solo_001.png")
        result.skipped shouldBe emptyList()
        result.failed shouldBe emptyList()
        result.unmatchedImages shouldBe emptyList()
    }

    @Test
    @DisplayName("이미 등록된 키는 파일이 없어도 ALREADY_REGISTERED 로 건너뛴다 (이미지 판정보다 먼저)")
    fun alreadyRegisteredBeforeImage() {
        val result = plan(rows = listOf(row(2, "friend_001")), existing = setOf("friend_001"))

        result.skipped shouldBe listOf(BulkRowOutcome(2, "friend_001", BulkSkipReason.ALREADY_REGISTERED))
        result.toCreate shouldBe emptyList()
    }

    @Test
    @DisplayName("이미지가 없는 행은 IMAGE_NOT_PROVIDED 로 건너뛴다 (couple)")
    fun imageNotProvided() {
        val result = plan(rows = listOf(row(2, "couple_001", tag = "couple")))

        result.skipped shouldBe listOf(BulkRowOutcome(2, "couple_001", BulkSkipReason.IMAGE_NOT_PROVIDED))
    }

    @Test
    @DisplayName("행 판정 실패 사유 - 키 형식, CSV 키 중복, 인원 수, 관계 값, 관계 비허용, 이미지 중복")
    fun failures() {
        val result = plan(
            rows = listOf(
                row(2, "bad key"),
                row(3, "dup"),
                row(4, "dup"),
                row(5, "hc", headCount = "TEN"),
                row(6, "rel", tag = "lover"),
                row(7, "one_rel", headCount = "ONE", tag = "friend"),
                row(8, "two_img"),
            ),
            images = listOf("dup.png", "hc.png", "rel.png", "one_rel.png", "two_img.png", "two_img.JPG"),
        )

        result.failed shouldBe listOf(
            BulkRowOutcome(2, "bad key", BulkFailReason.INVALID_KEY),
            BulkRowOutcome(3, "dup", BulkFailReason.DUPLICATE_KEY_IN_CSV),
            BulkRowOutcome(4, "dup", BulkFailReason.DUPLICATE_KEY_IN_CSV),
            BulkRowOutcome(5, "hc", BulkFailReason.INVALID_HEAD_COUNT),
            BulkRowOutcome(6, "rel", BulkFailReason.INVALID_RELATION),
            BulkRowOutcome(7, "one_rel", BulkFailReason.RELATION_NOT_ALLOWED),
            BulkRowOutcome(8, "two_img", BulkFailReason.DUPLICATE_IMAGE),
        )
        result.toCreate shouldBe emptyList()
        // 무효 행의 파일도 그 행의 후보이므로 unmatched 가 아니다
        result.unmatchedImages shouldBe emptyList()
    }

    @Test
    @DisplayName("키 형식 검사가 CSV 중복 검사보다 먼저다. 101자 키는 INVALID_KEY")
    fun keyLength() {
        val longKey: String = "a".repeat(101)
        val result = plan(
            rows = listOf(row(2, longKey), row(3, "a".repeat(100))),
            images = listOf(
                "a".repeat(100) + ".png",
            ),
        )

        result.failed shouldBe listOf(BulkRowOutcome(2, longKey, BulkFailReason.INVALID_KEY))
        result.toCreate.single().sourceKey shouldBe "a".repeat(100)
    }

    @Test
    @DisplayName("확장자 대소문자(.PNG)와 NFD 파일명도 매칭된다")
    fun caseAndNormalizationInsensitive() {
        val nfdName: String = Normalizer.normalize("포즈_001.png", Normalizer.Form.NFD)
        val result = plan(
            rows = listOf(row(2, "friend_001"), row(3, "friend_002", fileName = "포즈_001.png")),
            images = listOf("FRIEND_001.PNG", nfdName),
        )

        result.toCreate.map { it.sourceKey } shouldBe listOf("friend_001", "friend_002")
        result.unmatchedImages shouldBe emptyList()
    }

    @Test
    @DisplayName("media.csv 의 file_name 이 있으면 그 이름만 후보다 (sourceKey.png 는 매칭하지 않는다)")
    fun mappedFileNameOnly() {
        val result = plan(
            rows = listOf(row(2, "friend_001", fileName = "f1.png")),
            images = listOf("friend_001.png"),
        )

        result.skipped shouldBe listOf(BulkRowOutcome(2, "friend_001", BulkSkipReason.IMAGE_NOT_PROVIDED))
        result.unmatchedImages shouldBe listOf("friend_001.png")
    }

    @Test
    @DisplayName("멱등 재전송 - 이미 등록된 행의 파일은 unmatched 가 아니다. 어떤 행에도 해당하지 않는 파일만 unmatched")
    fun unmatchedOnlyForStrayFiles() {
        val result = plan(
            rows = listOf(row(2, "friend_001"), row(3, "friend_002")),
            images = listOf("friend_001.png", "friend_002.png", "stray.png"),
            existing = setOf("friend_001", "friend_002"),
        )

        result.skipped.map { it.reason } shouldBe List(2) { BulkSkipReason.ALREADY_REGISTERED }
        result.unmatchedImages shouldBe listOf("stray.png")
    }

    @Test
    @DisplayName("media.csv 의 두 키가 같은 file_name 을 가리키면 두 행 모두 IMAGE_SHARED_BY_ROWS (포즈 0건)")
    fun sharedFileByMediaCsv() {
        val result = plan(
            rows = listOf(row(2, "friend_001", fileName = "same.png"), row(3, "friend_002", fileName = "same.png")),
            images = listOf("same.png"),
        )

        result.failed shouldBe listOf(
            BulkRowOutcome(2, "friend_001", BulkFailReason.IMAGE_SHARED_BY_ROWS),
            BulkRowOutcome(3, "friend_002", BulkFailReason.IMAGE_SHARED_BY_ROWS),
        )
        result.toCreate shouldBe emptyList()
        result.unmatchedImages shouldBe emptyList()
    }

    @Test
    @DisplayName("대소문자만 다른 키(Friend_001/friend_001)는 같은 파일을 가리키므로 둘 다 IMAGE_SHARED_BY_ROWS")
    fun sharedFileByCaseOnlyKeys() {
        val result = plan(
            rows = listOf(row(2, "Friend_001"), row(3, "friend_001")),
            images = listOf("friend_001.png"),
        )

        result.failed.map { it.reason } shouldBe List(2) { BulkFailReason.IMAGE_SHARED_BY_ROWS }
        result.toCreate shouldBe emptyList()
    }

    @Test
    @DisplayName("공유 파일이라도 이미 등록됐거나 앞 단계에서 실패한 행은 원래 사유를 유지하고, 나머지 행만 IMAGE_SHARED_BY_ROWS")
    fun sharedFileKeepsEarlierOutcomes() {
        val result = plan(
            rows = listOf(
                row(2, "done", fileName = "same.png"),
                row(3, "bad_rel", tag = "lover", fileName = "same.png"),
                row(4, "fresh", fileName = "same.png"),
                row(5, "other"),
            ),
            images = listOf("same.png", "other.png"),
            existing = setOf("done"),
        )

        result.skipped shouldBe listOf(BulkRowOutcome(2, "done", BulkSkipReason.ALREADY_REGISTERED))
        result.failed shouldBe listOf(
            BulkRowOutcome(3, "bad_rel", BulkFailReason.INVALID_RELATION),
            BulkRowOutcome(4, "fresh", BulkFailReason.IMAGE_SHARED_BY_ROWS),
        )
        result.toCreate.map { it.sourceKey } shouldBe listOf("other")
    }

    @Test
    @DisplayName("공유 후보라도 그 파일이 올라오지 않았으면 IMAGE_NOT_PROVIDED")
    fun sharedCandidateNotUploaded() {
        val result = plan(
            rows = listOf(row(2, "a", fileName = "same.png"), row(3, "b", fileName = "same.png")),
            images = emptyList(),
        )

        result.skipped.map { it.reason } shouldBe List(2) { BulkSkipReason.IMAGE_NOT_PROVIDED }
        result.failed shouldBe emptyList()
    }
}
