package com.neki.domain.pose.service

import com.neki.core.code.ResultCode
import com.neki.core.domain.vo.CountedPage
import com.neki.core.exception.BusinessException
import com.neki.domain.pose.dto.PoseCommand
import com.neki.domain.pose.dto.PoseQuery
import com.neki.domain.pose.models.BulkFailReason
import com.neki.domain.pose.models.BulkRowOutcome
import com.neki.domain.pose.models.BulkSkipReason
import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.PlannedPose
import com.neki.domain.pose.models.Pose
import com.neki.domain.pose.models.PoseBulkUploadPlan
import com.neki.domain.pose.models.PoseImageFile
import com.neki.domain.pose.models.PoseRelation
import com.neki.domain.pose.repository.PoseRepository
import org.springframework.stereotype.Component
import java.text.Normalizer

/**
 * fileName       : PoseService
 * author         : koo
 * date           : 2026. 8. 3.
 * description    : 포즈 자체의 목록 조회와 등록.
 *                  스크랩은 [PoseScrapService], 무작위 추첨은 [RandomPoseService] 가 맡는다
 */
@Component
class PoseService(private val poseRepository: PoseRepository) {

    companion object {
        private val SOURCE_KEY_PATTERN: Regex = Regex("^[A-Za-z0-9._-]{1,100}$")

        // media.csv 매핑이 없을 때 sourceKey 뒤에 붙여 찾는 확장자
        private val DEFAULT_IMAGE_EXTENSIONS: List<String> = listOf(".png", ".jpg", ".jpeg")

        /**
         * 파일명 비교용 정규화. macOS 가 NFD 로 보내는 한글 파일명과 대소문자 차이(.PNG)를 흡수한다.
         */
        fun normalizeFileName(fileName: String): String =
            Normalizer.normalize(fileName.trim(), Normalizer.Form.NFC).lowercase()
    }

    /**
     * 사용자별 스크랩 정보를 붙이지 않고 전체 건수를 함께 센다.
     */
    fun listAllPoses(query: PoseQuery.GetAllPoses): CountedPage<Pose> = CountedPage(
        items = poseRepository.findAll(query),
        totalCount = poseRepository.count(query),
        size = query.pagination.size,
    )

    /**
     * 한 번의 업로드에 같은 media를 두 번 담을 수 없다.
     * 관계는 2인 포즈에만 붙일 수 있고, 하나라도 어기면 요청 전체를 거부한다.
     */
    fun createPoses(command: PoseCommand.UploadPoses): List<Pose> {
        validateNoDuplicateMediaIds(command.uploads)
        validateRelations(command.uploads)

        return command.uploads.map { upload ->
            Pose(
                userId = command.userId,
                mediaId = upload.mediaId,
                headCount = upload.headCount,
                memo = upload.memo,
                relation = upload.relation,
            )
        }
    }

    fun saveAll(poses: List<Pose>): List<Pose> = poseRepository.saveAll(poses)

    fun findExistingSourceKeys(sourceKeys: Collection<String>): Set<String> =
        if (sourceKeys.isEmpty()) emptySet() else poseRepository.findExistingSourceKeys(sourceKeys)

    /**
     * 일괄 업로드로 만들 포즈. 이미지는 이미 media 로 저장되어 있어야 한다.
     */
    fun createBulkPose(userId: Long, planned: PlannedPose, mediaId: Long): Pose = Pose(
        userId = userId,
        mediaId = mediaId,
        headCount = planned.headCount,
        relation = planned.relation,
        sourceKey = planned.sourceKey,
    )

    /**
     * 일괄 업로드 행을 판정한다. 저장소를 건드리지 않는 순수 로직이다.
     *
     * 파일 매칭은 판정과 무관하게 모든 행에 대해 먼저 한다. 그래서 이미 등록됐거나 잘못된 행의 파일은
     * unmatchedImages 로 잡히지 않고, 어떤 행의 후보에도 해당하지 않는 파일만 잡힌다.
     * 같은 기준(행의 유효·등록 여부와 무관)으로 올라온 파일 하나가 두 행 이상의 후보이면 그 파일을 "공유 파일" 로 본다.
     *
     * 행 판정 순서 (앞 단계에서 정해지면 뒤 단계는 보지 않는다):
     * 1. 키 형식 → INVALID_KEY
     * 2. CSV 안 키 중복 → DUPLICATE_KEY_IN_CSV
     * 3. 이미 등록됨 → ALREADY_REGISTERED (skipped)
     * 4. 인원 수 → INVALID_HEAD_COUNT
     * 5. 관계 → INVALID_RELATION, RELATION_NOT_ALLOWED
     * 6. 이미지 매칭: 0개 → IMAGE_NOT_PROVIDED (skipped), 2개 이상 → DUPLICATE_IMAGE,
     *    1개인데 공유 파일 → IMAGE_SHARED_BY_ROWS, 그 밖 → 등록
     * 따라서 공유 파일을 가리키는 행 중 이미 등록됐거나 앞 단계에서 실패한 행은 그 사유를 유지하고,
     * 나머지 행은 IMAGE_SHARED_BY_ROWS 로 실패한다. 한 이미지로 포즈가 두 건 생기지 않게 하기 위해서다.
     */
    fun planBulkUpload(command: PoseCommand.BulkUploadPoses, existingSourceKeys: Set<String>): PoseBulkUploadPlan {
        val imagesByName: Map<String, List<PoseImageFile>> = command.images.groupBy { normalizeFileName(it.fileName) }

        val candidatesByRow: Map<PoseCommand.BulkUploadPoses.Row, Set<String>> =
            command.rows.associateWith { candidateFileNames(it) }
        val allCandidates: Set<String> = candidatesByRow.values.flatten().toSet()

        val unmatchedImages: List<String> = command.images
            .filter { normalizeFileName(it.fileName) !in allCandidates }
            .map { it.fileName }

        val sharedFileNames: Set<String> = candidatesByRow.values
            .flatten()
            .filter { it in imagesByName }
            .groupingBy { it }
            .eachCount()
            .filter { it.value > 1 }
            .keys

        val duplicatedKeys: Set<String> = command.rows
            .map { it.sourceKey }
            .groupingBy { it }
            .eachCount()
            .filter { it.value > 1 }
            .keys

        val toCreate: MutableList<PlannedPose> = mutableListOf()
        val skipped: MutableList<BulkRowOutcome<BulkSkipReason>> = mutableListOf()
        val failed: MutableList<BulkRowOutcome<BulkFailReason>> = mutableListOf()

        command.rows.forEach { row ->
            val fail: (BulkFailReason) -> Unit = { failed += BulkRowOutcome(row.lineNumber, row.sourceKey, it) }
            val skip: (BulkSkipReason) -> Unit = { skipped += BulkRowOutcome(row.lineNumber, row.sourceKey, it) }

            if (!SOURCE_KEY_PATTERN.matches(row.sourceKey)) return@forEach fail(BulkFailReason.INVALID_KEY)
            if (row.sourceKey in duplicatedKeys) return@forEach fail(BulkFailReason.DUPLICATE_KEY_IN_CSV)
            if (row.sourceKey in existingSourceKeys) return@forEach skip(BulkSkipReason.ALREADY_REGISTERED)

            val headCount: HeadCount = parseHeadCount(row.headCount)
                ?: return@forEach fail(BulkFailReason.INVALID_HEAD_COUNT)

            val relation: PoseRelation? = try {
                PoseRelation.fromTag(row.tag)
            } catch (_: IllegalArgumentException) {
                return@forEach fail(BulkFailReason.INVALID_RELATION)
            }
            if (relation != null && !PoseRelation.isAllowedFor(headCount)) {
                return@forEach fail(BulkFailReason.RELATION_NOT_ALLOWED)
            }

            val matchedNames: List<String> = candidatesByRow.getValue(row).filter { it in imagesByName }
            val matched: List<PoseImageFile> = matchedNames.flatMap { imagesByName.getValue(it) }
            when {
                matched.isEmpty() -> skip(BulkSkipReason.IMAGE_NOT_PROVIDED)
                matched.size > 1 -> fail(BulkFailReason.DUPLICATE_IMAGE)
                matchedNames.single() in sharedFileNames -> fail(BulkFailReason.IMAGE_SHARED_BY_ROWS)
                else -> toCreate += PlannedPose(row.lineNumber, row.sourceKey, headCount, relation, matched.single())
            }
        }

        return PoseBulkUploadPlan(
            toCreate = toCreate,
            skipped = skipped,
            failed = failed,
            unmatchedImages = unmatchedImages,
        )
    }

    private fun candidateFileNames(row: PoseCommand.BulkUploadPoses.Row): Set<String> {
        val mapped: String? = row.fileName?.takeIf { it.isNotBlank() }
        if (mapped != null) return setOf(normalizeFileName(mapped))

        return DEFAULT_IMAGE_EXTENSIONS.map { normalizeFileName(row.sourceKey + it) }.toSet()
    }

    private fun parseHeadCount(raw: String): HeadCount? =
        HeadCount.entries.firstOrNull { it.name == raw.trim().uppercase() }

    private fun validateNoDuplicateMediaIds(uploads: List<PoseCommand.UploadPoses.Item>) {
        val duplicates: Set<Long> = uploads.map { it.mediaId }
            .groupingBy { it }
            .eachCount()
            .filter { it.value > 1 }
            .keys

        if (duplicates.isNotEmpty()) {
            throw BusinessException(ResultCode.INVALID_PARAMETER)
        }
    }

    private fun validateRelations(uploads: List<PoseCommand.UploadPoses.Item>) {
        val violated: Boolean = uploads.any { it.relation != null && !PoseRelation.isAllowedFor(it.headCount) }
        if (violated) {
            throw BusinessException(ResultCode.INVALID_PARAMETER)
        }
    }
}
