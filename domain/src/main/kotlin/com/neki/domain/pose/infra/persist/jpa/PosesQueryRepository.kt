package com.neki.domain.pose.infra.persist.jpa

import com.neki.core.domain.vo.SortOrder
import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.Pose
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
        sortOrder: SortOrder,
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
        .where(headCount?.let { pose.headCount.eq(it) })
        .orderBy(
            when (sortOrder) {
                SortOrder.ASC -> pose.createdAt.asc()
                SortOrder.DESC -> pose.createdAt.desc()
            },
        )
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

    fun incrementViewCount(poseId: Long): Long = queryFactory
        .update(pose)
        .set(pose.viewCount, pose.viewCount.add(1))
        .where(pose.id.eq(poseId))
        .execute()
}
