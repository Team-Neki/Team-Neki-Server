package com.neki.admin.pose.infra.persist.jpa

import com.neki.domain.pose.dto.PoseQuery
import com.neki.domain.pose.models.Pose
import com.neki.domain.pose.models.QPose.pose
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

/**
 * fileName       : PoseQueryRepository
 * author         : koo
 * date           : 2026. 8. 10.
 * description    : 어드민 포즈 목록 조회용 QueryDSL repository
 */
@Repository
class PoseQueryRepository(private val queryFactory: JPAQueryFactory) {

    fun findAll(query: PoseQuery.GetAllPoses): List<Pose> = queryFactory
        .selectFrom(pose)
        .where(*conditions(query))
        // 어드민 목록은 최신순 고정이라 Pagination.sortOrder 는 쓰지 않는다
        .orderBy(pose.createdAt.desc())
        .offset(query.pagination.offset.toLong())
        // 전체 건수를 따로 세므로 hasNext 판단용 초과 조회(Pagination.limit)가 필요 없다
        .limit(query.pagination.size.toLong())
        .fetch()

    fun count(query: PoseQuery.GetAllPoses): Long = queryFactory
        .select(pose.count())
        .from(pose)
        .where(*conditions(query))
        .fetchOne() ?: 0L

    private fun conditions(query: PoseQuery.GetAllPoses): Array<Predicate?> = arrayOf(
        query.headCount?.let { pose.headCount.eq(it) },
    )
}
