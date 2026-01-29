package com.yapp2app.pose.infra.persist.jpa

import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import com.yapp2app.common.domain.vo.SortOrder
import com.yapp2app.pose.application.contract.PoseWithScrap
import com.yapp2app.pose.domain.HeadCount
import com.yapp2app.pose.domain.entity.Pose
import com.yapp2app.pose.domain.entity.QPose.pose
import com.yapp2app.pose.domain.entity.QScrapPose.scrapPose
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
            scrapPose.id.userId.eq(pose.userId),
            scrapPose.id.poseId.eq(pose.id),
        )
        .where(
            pose.userId.eq(userId),
            pose.id.eq(poseId),
        )
        .fetchOne()

    fun findPoses(offset: Int, limit: Int, headCount: HeadCount?, sortOrder: SortOrder): List<Pose> = queryFactory
        .selectFrom(pose)
        .from(pose)
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

    fun countPoses(headCount: HeadCount): Long = queryFactory
        .select(pose.count())
        .from(pose)
        .where(pose.headCount.eq(headCount))
        .fetchOne() ?: 0L

    fun findPoseByOffset(offset: Long, headCount: HeadCount): Pose? = queryFactory
        .selectFrom(pose)
        .where(pose.headCount.eq(headCount))
        .offset(offset)
        .limit(1)
        .fetchOne()
}
