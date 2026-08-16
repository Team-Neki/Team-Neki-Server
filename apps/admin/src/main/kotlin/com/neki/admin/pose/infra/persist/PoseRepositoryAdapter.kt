package com.neki.admin.pose.infra.persist

import com.neki.admin.pose.infra.persist.jpa.JpaPoseRepository
import com.neki.admin.pose.infra.persist.jpa.PoseQueryRepository
import com.neki.core.domain.vo.SortOrder
import com.neki.domain.pose.dto.PoseQuery
import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.Pose
import com.neki.domain.pose.models.PoseWithScrap
import com.neki.domain.pose.repository.PoseRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : PoseRepositoryAdapter
 * author         : koo
 * date           : 2026. 8. 10.
 * description    : 어드민은 목록 조회·등록만 쓴다. 나머지는 사용자 컨텍스트가 필요한 api 전용 기능이다
 */
@Repository
class PoseRepositoryAdapter(
    private val jpaRepository: JpaPoseRepository,
    private val queryRepository: PoseQueryRepository,
) : PoseRepository {

    override fun findAll(query: PoseQuery.GetAllPoses): List<Pose> = queryRepository.findAll(query)

    override fun count(query: PoseQuery.GetAllPoses): Long = queryRepository.count(query)

    override fun getOwnedPoseWithScrap(userId: Long, poseId: Long): PoseWithScrap? = unsupported()

    override fun saveAll(poses: List<Pose>): List<Pose> = jpaRepository.saveAll(poses)

    override fun listPosesWithScrap(
        userId: Long,
        offset: Int,
        limit: Int,
        headCount: HeadCount?,
        sortOrder: SortOrder,
    ): List<PoseWithScrap> = unsupported()

    override fun listOwnedScrapPoses(userId: Long, offset: Int, limit: Int, sortOrder: SortOrder): List<Pose> =
        unsupported()

    override fun existsPose(poseId: Long): Boolean = unsupported()

    override fun countPoses(headCount: HeadCount, excludeIds: List<Long>): Long = unsupported()

    override fun findPoseByOffset(offset: Long, headCount: HeadCount, excludeIds: List<Long>): Pose? = unsupported()

    override fun incrementViewCount(poseId: Long) = unsupported()

    private fun unsupported(): Nothing =
        throw UnsupportedOperationException("apps:api 전용 기능이다. apps:admin 에서는 호출 경로가 없다.")
}
