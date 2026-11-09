package com.neki.api.pose.application

import com.neki.api.pose.application.dto.PoseResult
import com.neki.core.annotation.UseCase
import com.neki.core.transaction.TransactionRunner
import com.neki.domain.pose.client.MediaClient
import com.neki.domain.pose.dto.PoseCommand
import com.neki.domain.pose.models.BulkFailReason
import com.neki.domain.pose.models.BulkSkipReason
import com.neki.domain.pose.models.PlannedPose
import com.neki.domain.pose.models.Pose
import com.neki.domain.pose.models.PoseBulkUploadPlan
import com.neki.domain.pose.models.PoseImageStoreOutcome
import com.neki.domain.pose.service.PoseService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * fileName       : BulkUploadPosesUseCase
 * author         : darren
 * date           : 2026. 11. 9.
 * description    : PM 일괄 업로드(pose.csv + 이미지). 행마다 이미지 저장 → 포즈 저장을 따로 처리해 부분 성공을 보고한다.
 *                  source_key 기준으로 멱등이다. 이미 등록된 키는 건너뛴다.
 *                  CSV 순서대로 저장하므로 한 요청 안에서는 CSV 마지막 행이 목록 맨 위에 온다.
 */
@UseCase
class BulkUploadPosesUseCase(
    private val poseService: PoseService,
    private val mediaClient: MediaClient,
    private val transactionRunner: TransactionRunner,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun execute(command: PoseCommand.BulkUploadPoses): PoseResult.BulkUploadPoses {
        val sourceKeys: List<String> = command.rows.map { it.sourceKey }.distinct()
        val existing: Set<String> = transactionRunner.readOnly { poseService.findExistingSourceKeys(sourceKeys) }

        val plan: PoseBulkUploadPlan = poseService.planBulkUpload(command, existing)

        val created: MutableList<PoseResult.BulkUploadPoses.Created> = mutableListOf()
        val skipped: MutableList<PoseResult.BulkUploadPoses.Skipped> =
            plan.skipped.map { PoseResult.BulkUploadPoses.Skipped(it.sourceKey, it.lineNumber, it.reason) }
                .toMutableList()
        val failed: MutableList<PoseResult.BulkUploadPoses.Failed> =
            plan.failed.map { PoseResult.BulkUploadPoses.Failed(it.sourceKey, it.lineNumber, it.reason) }
                .toMutableList()

        plan.toCreate.forEach { planned ->
            val outcome: PoseImageStoreOutcome = mediaClient.storePoseImage(command.userId, planned.image)
            val mediaId: Long = when (outcome) {
                is PoseImageStoreOutcome.Failed -> {
                    failed += PoseResult.BulkUploadPoses.Failed(planned.sourceKey, planned.lineNumber, outcome.reason)
                    return@forEach
                }
                is PoseImageStoreOutcome.Stored -> outcome.mediaId
            }

            // 저장이 커밋된 뒤의 처리(id 읽기 등)는 try 밖에 둔다. 커밋된 포즈의 media 를 discard 하지 않기 위해서다
            val pose: Pose = try {
                savePose(command.userId, planned, mediaId)
            } catch (e: Exception) {
                discardQuietly(command.userId, mediaId, planned)

                if (isRegisteredMeanwhile(planned)) {
                    log.warn("[POSE_BULK_UPLOAD] concurrent registration key={}", planned.sourceKey)
                    skipped += PoseResult.BulkUploadPoses.Skipped(
                        planned.sourceKey,
                        planned.lineNumber,
                        BulkSkipReason.ALREADY_REGISTERED,
                    )
                } else {
                    log.error("[POSE_BULK_UPLOAD] pose save failed key={}", planned.sourceKey, e)
                    failed += PoseResult.BulkUploadPoses.Failed(
                        planned.sourceKey,
                        planned.lineNumber,
                        BulkFailReason.SAVE_FAILED,
                    )
                }
                return@forEach
            }

            created += PoseResult.BulkUploadPoses.Created(
                sourceKey = planned.sourceKey,
                poseId = pose.id!!,
                mediaId = mediaId,
                headCount = pose.headCount,
                relation = pose.relation,
            )
        }

        log.info(
            "[POSE_BULK_UPLOAD] userId={} rows={} created={} skipped={} failed={} unmatched={}",
            command.userId,
            command.rows.size,
            created.size,
            skipped.size,
            failed.size,
            plan.unmatchedImages.size,
        )

        return PoseResult.BulkUploadPoses(
            totalRows = command.rows.size,
            created = created,
            skipped = skipped.sortedBy { it.line },
            failed = failed.sortedBy { it.line },
            unmatchedImages = plan.unmatchedImages,
        )
    }

    private fun savePose(userId: Long, planned: PlannedPose, mediaId: Long): Pose {
        val saved: List<Pose> = transactionRunner.run {
            poseService.saveAll(listOf(poseService.createBulkPose(userId, planned, mediaId)))
        }
        return saved.first()
    }

    /**
     * 보상이 실패해도 다음 행은 계속 처리한다. 남은 media 는 로그로 추적한다.
     */
    private fun discardQuietly(userId: Long, mediaId: Long, planned: PlannedPose) {
        runCatching { mediaClient.discardPoseImage(userId, mediaId) }
            .onFailure {
                log.error(
                    "[POSE_BULK_UPLOAD] discard failed, orphan mediaId={} key={}",
                    mediaId,
                    planned.sourceKey,
                    it,
                )
            }
    }

    /**
     * 저장 실패가 같은 키의 동시 등록 때문인지 확인한다. 재조회까지 실패하면 경합이 아닌 것으로 본다.
     */
    private fun isRegisteredMeanwhile(planned: PlannedPose): Boolean = runCatching {
        transactionRunner.readOnly { poseService.findExistingSourceKeys(listOf(planned.sourceKey)) }.isNotEmpty()
    }.getOrDefault(false)
}
