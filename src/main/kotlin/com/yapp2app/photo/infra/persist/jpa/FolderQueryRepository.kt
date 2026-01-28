package com.yapp2app.photo.infra.persist.jpa

import com.querydsl.core.types.Projections
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import com.yapp2app.photo.application.contract.FolderWithStats
import com.yapp2app.photo.domain.entity.QFolder.folder
import com.yapp2app.photo.domain.entity.QPhotoImage
import com.yapp2app.photo.domain.entity.QPhotoImage.photoImage
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * fileName       : FolderQueryRepository
 * author         : koo
 * date           : 2026. 1. 16. 오후 10:48
 * description    : Folder Querydsl 구현체
 */
@Repository
class FolderQueryRepository(private val queryFactory: JPAQueryFactory, private val entityManager: EntityManager) {

    fun deleteOwnedFolders(userId: Long, folderIds: List<Long>): Int {
        if (folderIds.isEmpty()) return 0

        return queryFactory
            .delete(folder)
            .where(folder.userId.eq(userId), folder.id.`in`(folderIds))
            .execute().toInt()
    }

    fun findOwnedFoldersWithStats(userId: Long): List<FolderWithStats> {
        val coverPhoto = QPhotoImage("coverPhoto")
        val latestPhotoSubquery = QPhotoImage("latestPhotoSubquery")

        return queryFactory
            .select(
                Projections.constructor(
                    FolderWithStats::class.java,
                    folder.id,
                    folder.name,
                    coverPhoto.mediaId,
                    JPAExpressions
                        .select(latestPhotoSubquery.mediaId)
                        .from(latestPhotoSubquery)
                        .where(
                            latestPhotoSubquery.folderId.eq(folder.id),
                            latestPhotoSubquery.userId.eq(userId),
                        )
                        .orderBy(latestPhotoSubquery.createdAt.desc())
                        .limit(1),
                    photoImage.id.count(),
                ),
            )
            .from(folder)
            .leftJoin(photoImage).on(
                photoImage.folderId.eq(folder.id),
                photoImage.userId.eq(userId),
            )
            .leftJoin(coverPhoto).on(
                coverPhoto.id.eq(folder.coverPhotoId),
                coverPhoto.userId.eq(userId),
            )
            .where(folder.userId.eq(userId))
            .groupBy(folder.id, folder.name, coverPhoto.mediaId)
            .fetch()
    }

    /**
     * 조건부 커버 업데이트: 새 사진이 더 최신일 때만 업데이트
     * 동시 업로드 Race Condition 방지
     */
    fun updateCoverPhotoIfNewer(
        userId: Long,
        folderId: Long,
        newCoverPhotoId: Long,
        newCoverPhotoCreatedAt: LocalDateTime,
    ): Int = queryFactory
        .update(folder)
        .set(folder.coverPhotoId, newCoverPhotoId)
        .set(folder.coverPhotoCreatedAt, newCoverPhotoCreatedAt)
        .where(
            folder.userId.eq(userId),
            folder.id.eq(folderId),
            // 기존 커버가 없거나, 새 사진이 더 최신인 경우만 업데이트
            folder.coverPhotoCreatedAt.isNull
                .or(folder.coverPhotoCreatedAt.lt(newCoverPhotoCreatedAt)),
        )
        .execute()
        .toInt()

    /**
     * 폴더별 최신 사진으로 커버 재계산
     * PostgreSQL의 DISTINCT ON + UPDATE FROM 활용
     */
    fun recalculateCoverPhotos(userId: Long, folderIds: List<Long>): Int {
        if (folderIds.isEmpty()) return 0

        // 1. 먼저 해당 폴더들의 커버를 모두 NULL로 초기화
        val nullifyQuery = entityManager.createNativeQuery(
            """
            UPDATE TB_FOLDER
            SET cover_photo_id = NULL, cover_photo_created_at = NULL
            WHERE user_id = :userId AND id IN (:folderIds)
            """.trimIndent(),
        )
        nullifyQuery.setParameter("userId", userId)
        nullifyQuery.setParameter("folderIds", folderIds)
        nullifyQuery.executeUpdate()

        // 2. 최신 사진이 있는 폴더만 커버 업데이트
        val updateQuery = entityManager.createNativeQuery(
            """
            UPDATE TB_FOLDER f
            SET
                cover_photo_id = latest.photo_id,
                cover_photo_created_at = latest.created_at
            FROM (
                SELECT DISTINCT ON (p.folder_id)
                    p.folder_id,
                    p.id AS photo_id,
                    p.created_at
                FROM TB_PHOTO_IMAGE p
                WHERE p.user_id = :userId
                  AND p.folder_id IN (:folderIds)
                ORDER BY p.folder_id, p.created_at DESC
            ) AS latest
            WHERE f.id = latest.folder_id
              AND f.user_id = :userId
            """.trimIndent(),
        )
        updateQuery.setParameter("userId", userId)
        updateQuery.setParameter("folderIds", folderIds)
        return updateQuery.executeUpdate()
    }
}
