package com.neki.domain.pose.infra.persist

import com.neki.core.code.ResultCode
import com.neki.core.domain.vo.SortOrder
import com.neki.core.exception.BusinessException
import com.neki.domain.pose.dto.PoseQuery
import com.neki.domain.pose.infra.persist.jpa.JpaPoseRepository
import com.neki.domain.pose.infra.persist.jpa.PosesQueryRepository
import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.Pose
import com.neki.domain.pose.models.PoseWithScrap
import com.neki.domain.pose.repository.PoseRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

/**
 * fileName       : PoseRepositoryAdapter
 * author         : darren
 * date           : 2026. 1. 27. 17:11
 * description    :
 */
@Repository
class PoseRepositoryAdapter(
    private val jpaRepository: JpaPoseRepository,
    private val queryRepository: PosesQueryRepository,
) : PoseRepository {

    override fun getOwnedPoseWithScrap(userId: Long, poseId: Long): PoseWithScrap? =
        queryRepository.findOwnedPoseWithScrap(userId, poseId)

    override fun saveAll(poses: List<Pose>): List<Pose> = jpaRepository.saveAll(poses)

    override fun findById(poseId: Long): Pose =
        jpaRepository.findByIdOrNull(poseId) ?: throw BusinessException(ResultCode.NOT_FOUND)

    override fun listPosesWithScrap(
        userId: Long,
        offset: Int,
        limit: Int,
        headCount: HeadCount?,
        sortOrder: SortOrder,
    ): List<PoseWithScrap> = queryRepository.listPosesWithScrap(userId, offset, limit, headCount, sortOrder)

    override fun listOwnedScrapPoses(userId: Long, offset: Int, limit: Int, sortOrder: SortOrder): List<Pose> =
        queryRepository.findOwnedScrapPoses(userId, offset, limit, sortOrder)

    // 어드민 목록 조회 전용이라 apps:api 에서는 호출 경로가 없다.
    override fun findAll(query: PoseQuery.GetAllPoses): List<Pose> =
        throw UnsupportedOperationException("어드민 전용 조회다. apps:admin 의 어댑터를 쓴다.")

    override fun count(query: PoseQuery.GetAllPoses): Long =
        throw UnsupportedOperationException("어드민 전용 조회다. apps:admin 의 어댑터를 쓴다.")

    override fun existsPose(poseId: Long): Boolean = jpaRepository.existsById(poseId)

    override fun countPoses(headCount: HeadCount, excludeIds: List<Long>): Long =
        queryRepository.countPoses(headCount, excludeIds)

    override fun findPoseByOffset(offset: Long, headCount: HeadCount, excludeIds: List<Long>): Pose? =
        queryRepository.findPoseByOffset(offset, headCount, excludeIds)

    override fun incrementViewCount(poseId: Long) {
        queryRepository.incrementViewCount(poseId)
    }
}
