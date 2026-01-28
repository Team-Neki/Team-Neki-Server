package com.yapp2app.pose.infra.persist.jpa

import com.querydsl.jpa.impl.JPAQueryFactory
import com.yapp2app.common.domain.vo.SortOrder
import com.yapp2app.pose.domain.entity.Pose
import com.yapp2app.pose.domain.entity.QPose.pose
import org.springframework.stereotype.Repository

/**
 * fileName       : PosesQueryRepository
 * author         : darren
 * date           : 2026. 1. 28. 14:32
 * description    : Pose QueryDSL repository for pagination
 */
@Repository
class PosesQueryRepository(private val queryFactory: JPAQueryFactory) {

    fun findPoses(offset: Int, limit: Int, sortOrder: SortOrder): List<Pose> = queryFactory
        .selectFrom(pose)
        .from(pose)
        .orderBy(
            when (sortOrder) {
                SortOrder.ASC -> pose.createdAt.asc()
                SortOrder.DESC -> pose.createdAt.desc()
            },
        )
        .offset(offset.toLong())
        .limit(limit.toLong())
        .fetch()
}
