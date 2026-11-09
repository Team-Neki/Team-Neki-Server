package com.neki.domain.pose.infra.persist.jpa

import com.neki.core.domain.vo.SortOrder
import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.Pose
import com.neki.domain.pose.models.PoseRelation
import com.neki.domain.pose.models.PoseWithScrap
import com.neki.domain.pose.models.QPose.pose
import com.neki.domain.pose.models.QScrapPose.scrapPose
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

/**
 * fileName       : PosesQueryRepository
 * author         : darren
 * date           : 2026. 1. 28. 14:32
 * description    : Pose QueryDSL repository for pagination
 */
@Repository
class PosesQueryRepository(private val queryFactory: JPAQueryFactory) {

    fun findOwnedPoseWithScrap(userId: Long, poseId: Long): PoseWithScrap? = queryFactory
        .select(
            Projections.constructor(
                PoseWithScrap::class.java,
                pose,
                CaseBuilder()
                    .`when`(scrapPose.id.poseId.isNotNull).then(true)
                    .otherwise(false),
            ),
        )
        .from(pose)
        .leftJoin(scrapPose)
        .on(
            scrapPose.id.userId.eq(userId),
            scrapPose.id.poseId.eq(pose.id),
        )
        .where(
            pose.id.eq(poseId),
        )
        .fetchOne()

    fun listPosesWithScrap(
        userId: Long,
        offset: Int,
        limit: Int,
        headCount: HeadCount?,
        relation: PoseRelation?,
    ): List<PoseWithScrap> = queryFactory
        .select(
            Projections.constructor(
                PoseWithScrap::class.java,
                pose,
                CaseBuilder()
                    .`when`(scrapPose.id.poseId.isNotNull).then(true)
                    .otherwise(false),
            ),
        )
        .from(pose)
        .leftJoin(scrapPose).on(
            scrapPose.id.poseId.eq(pose.id),
            scrapPose.id.userId.eq(userId),
        )
        .where(
            headCount?.let { pose.headCount.eq(it) },
            relation?.let { pose.relation.eq(it) },
        )
        // 위키 8-1: 모든 탭 최신순 고정. 등록 시각이 같으면 나중에 들어온 포즈(id 가 큰 쪽)가 먼저다
        .orderBy(pose.createdAt.desc(), pose.id.desc())
        .offset(offset.toLong())
        .limit(limit.toLong())
        .fetch()

    fun findOwnedScrapPoses(userId: Long, offset: Int, limit: Int, sortOrder: SortOrder): List<Pose> = queryFactory
        .selectFrom(pose)
        .innerJoin(scrapPose)
        .on(
            scrapPose.id.poseId.eq(pose.id),
        )
        .where(
            scrapPose.id.userId.eq(userId),
        )
        .orderBy(
            when (sortOrder) {
                SortOrder.ASC -> scrapPose.createdAt.asc()
                SortOrder.DESC -> scrapPose.createdAt.desc()
            },
        )
        .offset(offset.toLong())
        .limit(limit.toLong())
        .fetch()

    fun findExistingSourceKeys(sourceKeys: Collection<String>): Set<String> {
        if (sourceKeys.isEmpty()) return emptySet()

        return queryFactory
            .select(pose.sourceKey)
            .from(pose)
            .where(pose.sourceKey.`in`(sourceKeys))
            .fetch()
            .filterNotNull()
            .toSet()
    }

    fun countPoses(headCount: HeadCount, excludeIds: List<Long>): Long = queryFactory
        .select(pose.count())
        .from(pose)
        .where(
            pose.headCount.eq(headCount),
            excludeIds.takeIf { it.isNotEmpty() }?.let { pose.id.notIn(it) },
        )
        .fetchOne() ?: 0L

    fun findPoseByOffset(offset: Long, headCount: HeadCount, excludeIds: List<Long>): Pose? = queryFactory
        .selectFrom(pose)
        .where(
            pose.headCount.eq(headCount),
            excludeIds.takeIf { it.isNotEmpty() }?.let { pose.id.notIn(it) },
        )
        .offset(offset)
        .limit(1)
        .fetchOne()
}
