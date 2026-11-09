package com.neki.domain.pose.models

/**
 * fileName       : PoseBulkUpload
 * author         : darren
 * date           : 2026. 11. 9.
 * description    : PM 일괄 업로드(pose.csv + 이미지)의 행별 판정 결과
 */
data class PoseBulkUploadPlan(
    val toCreate: List<PlannedPose>,
    val skipped: List<BulkRowOutcome<BulkSkipReason>>,
    val failed: List<BulkRowOutcome<BulkFailReason>>,
    val unmatchedImages: List<String>,
)

/**
 * 만들 포즈 한 건. 이미지는 아직 저장되지 않았다.
 */
data class PlannedPose(
    val lineNumber: Int,
    val sourceKey: String,
    val headCount: HeadCount,
    val relation: PoseRelation?,
    val image: PoseImageFile,
)

data class BulkRowOutcome<R>(val lineNumber: Int, val sourceKey: String, val reason: R)

/**
 * 건너뛴 행. 다시 보내도 결과가 같은 정상 상황이다.
 */
enum class BulkSkipReason {
    // 같은 source_key 의 포즈가 이미 있다 (멱등 재전송)
    ALREADY_REGISTERED,

    // 이 요청에 행에 맞는 이미지가 없다 (예: 아직 받지 못한 couple)
    IMAGE_NOT_PROVIDED,
}

/**
 * 실패한 행. 데이터나 파일을 고친 뒤 다시 보내야 한다.
 */
enum class BulkFailReason {
    INVALID_KEY,
    DUPLICATE_KEY_IN_CSV,
    INVALID_HEAD_COUNT,
    INVALID_RELATION,
    RELATION_NOT_ALLOWED,

    // 한 행에 맞는 이미지가 둘 이상 (예: a.png 와 a.jpg)
    DUPLICATE_IMAGE,

    // 이미지 하나가 CSV 의 두 행 이상에 해당한다 (media.csv 가 같은 file_name 을 가리키거나 대소문자만 다른 키)
    IMAGE_SHARED_BY_ROWS,
    UNSUPPORTED_IMAGE,
    STORAGE_FAILED,
    SAVE_FAILED,
}
