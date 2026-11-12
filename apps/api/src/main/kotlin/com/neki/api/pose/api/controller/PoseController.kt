package com.neki.api.pose.api.controller

import com.neki.api.common.api.document.RequiresSecurity
import com.neki.api.pose.api.dto.PoseConverter
import com.neki.api.pose.api.dto.PoseRequest
import com.neki.api.pose.api.dto.PoseResponse
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
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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

    private val requestConverter: PoseConverter.RequestConverter,
    private val responseConverter: PoseConverter.ResponseConverter,
) {

    @Operation(
        summary = "포즈 등록 API",
        description = """
            ADMIN 권한이 있는 사용자만 호출이 가능합니다.
            관리자가 포즈를 업로드할 때 해당 API를 사용합니다.
            relation: 선택. headCount 가 TWO 일 때만 허용하며, 위반하면 요청 전체가 400 D-01 입니다.
            sourceKey: 선택. 형식 ^[A-Za-z0-9._-]{1,100}$, 대소문자 구분. 요청 안에서 중복되면 400 D-01 입니다.
            이미 등록된 sourceKey 가 하나라도 있으면 요청 전체가 409 D-18 입니다. 포즈는 만들지 않고 media 는 업로드 확인 전 상태로 남습니다.
            """,
    )
    @PostMapping("/admin/upload")
    fun uploadPoses(
        @AuthenticationPrincipal(expression = "id") ownerId: Long,
        @Valid @RequestBody request: PoseRequest.UploadPose,
    ): BaseResponse<Any> {
        val command: PoseCommand.UploadPoses = requestConverter.toUploadPosesCommand(ownerId, request)

        uploadPosesUseCase.execute(command)

        return BaseResponse()
    }

    @Operation(
        summary = "포즈 목록 API",
        description = """
            포즈 목록을 조회합니다. Offset 기반 페이징을 지원합니다.
            필터는 filterGroup 으로 보내며, 필터를 안 걸려면 {} 를 보냅니다. filterGroup 이 없으면 400 D-01 입니다.

            filterGroup.headCountFilter.headCount (없으면 전체):
            * ONE("1인")
            * TWO("2인")
            * THREE("3인")
            * FOUR("4인")
            * FIVE_OR_MORE("5인 이상")

            filterGroup.relationFilter.relation (2인 탭의 관계 필터, 없으면 관계 없는 포즈 포함 전체):
            * FRIEND("친구")
            * COUPLE("커플")
            * SSUM("썸")
            관계는 2인 포즈에만 있으므로 TWO 가 아닌 headCount 와 함께 보내면 빈 목록입니다.

            정렬: 최신순(등록일 내림차순, 같으면 포즈 ID 내림차순) 고정입니다.
        """,
    )
    @PostMapping("/filter")
    fun filterPoses(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: PoseRequest.GetPoses,
    ): BaseResponse<PoseResponse.GetPoses> {
        val query: PoseQuery.GetPoses = requestConverter.toGetPosesQuery(userId, request)

        val result: PoseResult.GetPoses = getPosesUseCase.execute(query)

        val response: PoseResponse.GetPoses = responseConverter.toGetPosesResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "포즈 목록 API (deprecated)",
        deprecated = true,
        description = """
            POST /api/poses/filter 를 사용하세요. 기존 앱 호환을 위해 남겨 둔 API 입니다.
            관계 필터는 지원하지 않습니다. 정렬은 최신순 고정이며 sortOrder 는 무시합니다.
        """,
    )
    @GetMapping
    fun poseList(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(required = false) headCount: HeadCount?,
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
