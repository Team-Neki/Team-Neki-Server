package com.neki.api.pose.api.controller

import com.neki.api.common.api.document.RequiresSecurity
import com.neki.api.pose.api.dto.PoseConverter
import com.neki.api.pose.api.dto.PoseRequest
import com.neki.api.pose.api.dto.PoseResponse
import com.neki.api.pose.application.BulkUploadPosesUseCase
import com.neki.api.pose.application.GetPoseUseCase
import com.neki.api.pose.application.GetPosesUseCase
import com.neki.api.pose.application.RandomPoseUseCase
import com.neki.api.pose.application.UploadPosesUseCase
import com.neki.api.pose.application.dto.PoseResult
import com.neki.core.api.dto.BaseResponse
import com.neki.core.domain.vo.SortOrder
import com.neki.domain.pose.dto.PoseCommand
import com.neki.domain.pose.dto.PoseQuery
import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.PoseRelation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

// 같은 path+method 의 두 핸들러(JSON, multipart)는 Swagger 에서 operation 하나로 합쳐지고 한쪽 설명만 남는다.
// 그래서 두 핸들러가 같은 설명을 쓴다.
private const val UPLOAD_SUMMARY = "포즈 등록 API (JSON 단건 등록 / multipart CSV 일괄 업로드)"
private const val UPLOAD_DESCRIPTION = """
    ADMIN 권한이 있는 사용자만 호출이 가능합니다. Content-Type 으로 두 방식을 구분합니다.

    1) application/json : 미리 올린 media 의 ID 로 포즈를 만듭니다.
    * relation(선택): 2인 포즈의 관계 FRIEND/COUPLE/SSUM. headCount 가 TWO 가 아닌데 보내면 요청 전체가 400 D-01 입니다.

    2) multipart/form-data : PM 일괄 업로드 (서버가 이미지를 S3 에 올리고 media 와 포즈를 만듭니다).
    * poseCsv (필수): 헤더 media_key,head_count,tag. tag 는 friend/couple/ssum 또는 빈 값
    * mediaCsv (선택): 헤더 media_key,file_name. 없으면 media_key + .png/.jpg/.jpeg 로 이미지를 찾습니다
    * images (0..N): PNG/JPEG. 형식과 크기는 서버가 파일에서 직접 읽습니다
    * media_key 가 이미 등록된 행은 건너뜁니다(멱등). 이미지가 없는 행도 건너뛰므로 받은 이미지만 나눠 올려도 됩니다.
    * 한도: 파일 10MB, 요청 128MB (413 D-17), part 100개 (413 D-18)

    curl -X POST {server}/api/poses/admin/upload -H "Authorization: Bearer {token}"
      -F "poseCsv=@pose.csv;type=text/csv" -F "mediaCsv=@media.csv;type=text/csv"
      -F "images=@friend_001_F1-4_F2-4.png;type=image/png" -F "images=@friend_002_M1-5_M2-2.png;type=image/png"
"""

/**
 * fileName       : PoseController
 * author         : darren
 * date           : 2026. 1. 27. 17:46
 * description    : Pose api endpoint
 */
@RequiresSecurity
@Tag(name = "pose image", description = "포즈 사진 API")
@RestController
@RequestMapping("/api/poses")
class PoseController(
    private val uploadPosesUseCase: UploadPosesUseCase,
    private val getPosesUseCase: GetPosesUseCase,
    private val getPoseUseCase: GetPoseUseCase,
    private val randomPoseUseCase: RandomPoseUseCase,
    private val bulkUploadPosesUseCase: BulkUploadPosesUseCase,

    private val requestConverter: PoseConverter.RequestConverter,
    private val responseConverter: PoseConverter.ResponseConverter,
) {

    @Operation(summary = UPLOAD_SUMMARY, description = UPLOAD_DESCRIPTION)
    @PostMapping("/admin/upload", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun uploadPoses(
        @AuthenticationPrincipal(expression = "id") ownerId: Long,
        @Valid @RequestBody request: PoseRequest.UploadPose,
    ): BaseResponse<Any> {
        val command: PoseCommand.UploadPoses = requestConverter.toUploadPosesCommand(ownerId, request)

        uploadPosesUseCase.execute(command)

        return BaseResponse()
    }

    @Operation(summary = UPLOAD_SUMMARY, description = UPLOAD_DESCRIPTION)
    @PostMapping("/admin/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun bulkUploadPoses(
        @AuthenticationPrincipal(expression = "id") ownerId: Long,
        @RequestPart("poseCsv") poseCsv: MultipartFile,
        @RequestPart("mediaCsv", required = false) mediaCsv: MultipartFile?,
        @RequestPart("images", required = false) images: List<MultipartFile>?,
    ): BaseResponse<PoseResponse.BulkUploadPoses> {
        val command: PoseCommand.BulkUploadPoses =
            requestConverter.toBulkUploadPosesCommand(ownerId, poseCsv, mediaCsv, images.orEmpty())

        val result: PoseResult.BulkUploadPoses = bulkUploadPosesUseCase.execute(command)

        val response: PoseResponse.BulkUploadPoses = responseConverter.toBulkUploadResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "포즈 목록 API",
        description = """
            포즈 목록을 조회합니다. Offset 기반 페이징을 지원합니다.

            headCount:
            * 없이 보내면 전체 조회
            * ONE("1인")
            * TWO("2인")
            * THREE("3인")
            * FOUR("4인")
            * FIVE_OR_MORE("5인 이상")

            relation (선택, 2인 탭의 관계 필터):
            * 없이 보내면 전체 (관계 없는 기존 포즈 포함)
            * FRIEND("친구")
            * COUPLE("커플")
            * SSUM("썸")
            관계는 2인 포즈에만 있으므로 TWO 가 아닌 headCount 와 함께 보내면 빈 목록입니다.

            정렬: 최신순(등록일 내림차순, 같으면 포즈 ID 내림차순) 고정입니다. sortOrder 는 무시합니다(deprecated).
        """,
    )
    @GetMapping
    fun poseList(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(required = false) headCount: HeadCount?,
        @RequestParam(required = false) relation: PoseRelation?,
        @Parameter(deprecated = true, description = "무시합니다. 정렬은 최신순 고정")
        @RequestParam(required = false)
        @Suppress("UNUSED_PARAMETER")
        sortOrder: SortOrder?,
    ): BaseResponse<PoseResponse.GetPoses> {
        val query: PoseQuery.GetPoses = requestConverter.toGetPosesQuery(
            userId = userId,
            page = page,
            size = size,
            headCount = headCount,
            relation = relation,
        )

        val result: PoseResult.GetPoses = getPosesUseCase.execute(query)

        val response: PoseResponse.GetPoses = responseConverter.toGetPosesResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "포즈 상세 조회 API",
        description = "포즈 상세 정보를 조회합니다.",
    )
    @GetMapping("/{poseId}")
    fun poseDetail(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @PathVariable poseId: Long,
    ): BaseResponse<PoseResponse.GetPose> {
        val query: PoseQuery.GetPose = requestConverter.toGetPoseQuery(userId, poseId)

        val result: PoseResult.GetPose = getPoseUseCase.execute(query)

        val response: PoseResponse.GetPose = responseConverter.toGetPoseResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "랜덤 포즈 조회 API",
        description = "랜덤 포즈를 임의로 1개 가져옵니다.",
    )
    @GetMapping("/random")
    fun randomPose(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @RequestParam(required = true) headCount: HeadCount,
        @RequestParam(required = false, defaultValue = "") excludeIds: String,
    ): BaseResponse<PoseResponse.GetPose> {
        val query: PoseQuery.GetRandomPose = requestConverter.toGetRandomPoseQuery(userId, headCount, excludeIds)

        val result: PoseResult.GetPose = randomPoseUseCase.execute(query)

        val response: PoseResponse.GetPose = responseConverter.toGetPoseResponse(result)

        return BaseResponse(data = response)
    }
}
