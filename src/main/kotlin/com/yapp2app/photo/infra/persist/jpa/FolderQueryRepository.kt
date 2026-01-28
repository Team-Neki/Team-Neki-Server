package com.yapp2app.photo.infra.persist.jpa

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
        // Query 1: 폴더 기본 정보 + 커버 사진 mediaId
        val coverPhoto = QPhotoImage("coverPhoto")
        val foldersWithCover = queryFactory
            .select(folder.id, folder.name, coverPhoto.mediaId)
            .from(folder)
            .leftJoin(coverPhoto).on(coverPhoto.id.eq(folder.coverPhotoId))
            .where(folder.userId.eq(userId))
            .fetch()

        if (foldersWithCover.isEmpty()) return emptyList()

        val folderIds = foldersWithCover.mapNotNull { it.get(folder.id) }

        // Query 2: 폴더별 사진 개수 (GROUP BY)
        val photoCountMap = queryFactory
            .select(photoImage.folderId, photoImage.id.count())
            .from(photoImage)
            .where(
                photoImage.userId.eq(userId),
                photoImage.folderId.`in`(folderIds),
            )
            .groupBy(photoImage.folderId)
            .fetch()
            .associate { it.get(photoImage.folderId)!! to it.get(photoImage.id.count())!! }

        // Query 3: 폴더별 최신 사진 mediaId (MAX(id)로 최신 사진 결정)
        val latestPhoto = QPhotoImage("latestPhoto")
        val maxIdSubquery = QPhotoImage("maxIdSubquery")
        val latestPhotoMap = queryFactory
            .select(latestPhoto.folderId, latestPhoto.mediaId)
            .from(latestPhoto)
            .where(
                latestPhoto.userId.eq(userId),
                latestPhoto.folderId.`in`(folderIds),
                latestPhoto.id.eq(
                    JPAExpressions
                        .select(maxIdSubquery.id.max())
                        .from(maxIdSubquery)
                        .where(
                            maxIdSubquery.folderId.eq(latestPhoto.folderId),
                            maxIdSubquery.userId.eq(userId),
                        ),
                ),
            )
            .fetch()
            .associate { it.get(latestPhoto.folderId)!! to it.get(latestPhoto.mediaId)!! }

        // 애플리케이션에서 조합
        return foldersWithCover.map { tuple ->
            val folderId = tuple.get(folder.id)!!
            FolderWithStats(
                folderId = folderId,
                name = tuple.get(folder.name)!!,
                coverPhotoMediaId = tuple.get(coverPhoto.mediaId),
                latestPhotoMediaId = latestPhotoMap[folderId],
                photoCount = photoCountMap[folderId] ?: 0L,
            )
        }
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
     * H2와 PostgreSQL 모두 호환되는 쿼리 사용
     * ID를 기준으로 최신 사진 결정 (createdAt이 동일한 경우에도 일관성 보장)
     */
    fun recalculateCoverPhotos(userId: Long, folderIds: List<Long>): Int {
        if (folderIds.isEmpty()) return 0

        // 1. 먼저 해당 폴더들의 커버를 모두 NULL로 초기화
        queryFactory
            .update(folder)
            .setNull(folder.coverPhotoId)
            .setNull(folder.coverPhotoCreatedAt)
            .where(folder.userId.eq(userId), folder.id.`in`(folderIds))
            .execute()

        // 2. 각 폴더의 최신 사진 조회 (ID가 가장 큰 사진 = 가장 최근 생성된 사진)
        val latestPhoto = QPhotoImage("latestPhoto")
        val maxIdSubquery = QPhotoImage("maxIdSubquery")

        val latestPhotos = queryFactory
            .select(latestPhoto.folderId, latestPhoto.id, latestPhoto.createdAt)
            .from(latestPhoto)
            .where(
                latestPhoto.userId.eq(userId),
                latestPhoto.folderId.`in`(folderIds),
                latestPhoto.id.eq(
                    JPAExpressions
                        .select(maxIdSubquery.id.max())
                        .from(maxIdSubquery)
                        .where(
                            maxIdSubquery.folderId.eq(latestPhoto.folderId),
                            maxIdSubquery.userId.eq(userId),
                        ),
                ),
            )
            .fetch()

        // 3. 각 폴더의 커버 업데이트
        var updateCount = 0
        for (tuple in latestPhotos) {
            val folderId = tuple.get(latestPhoto.folderId)!!
            val photoId = tuple.get(latestPhoto.id)!!
            val createdAt = tuple.get(latestPhoto.createdAt)!!

            updateCount += queryFactory
                .update(folder)
                .set(folder.coverPhotoId, photoId)
                .set(folder.coverPhotoCreatedAt, createdAt)
                .where(folder.userId.eq(userId), folder.id.eq(folderId))
                .execute()
                .toInt()
        }

        return updateCount
    }
}
