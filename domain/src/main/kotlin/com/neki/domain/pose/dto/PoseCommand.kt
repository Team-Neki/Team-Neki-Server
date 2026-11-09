package com.neki.domain.pose.dto

import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.PoseImageFile
import com.neki.domain.pose.models.PoseRelation

/**
 * fileName       : PoseCommand
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Pose domain command
 */
object PoseCommand {
    data class UploadPoses(val userId: Long?, val uploads: List<Item>) {
        data class Item(
            val mediaId: Long,
            val headCount: HeadCount,
            val memo: String?,
            val relation: PoseRelation? = null,
        )
    }

    /**
     * PM 일괄 업로드. 행은 CSV 원시 문자열 그대로 담고 판정은 [com.neki.domain.pose.service.PoseService.planBulkUpload] 가 한다.
     */
    data class BulkUploadPoses(val userId: Long, val rows: List<Row>, val images: List<PoseImageFile>) {
        /**
         * @param lineNumber CSV 의 물리 줄 번호 (헤더가 1)
         * @param fileName media.csv 로 매핑한 파일명. 없으면 sourceKey 로 찾는다
         */
        data class Row(
            val lineNumber: Int,
            val sourceKey: String,
            val headCount: String,
            val tag: String?,
            val fileName: String? = null,
        )
    }

    data class UpdatePoseScrap(val userId: Long, val poseId: Long, val scrap: Boolean)
}
