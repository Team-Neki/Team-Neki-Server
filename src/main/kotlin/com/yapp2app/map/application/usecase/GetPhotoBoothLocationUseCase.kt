package com.yapp2app.map.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.transaction.TransactionRunner
import com.yapp2app.map.application.command.GetPointLocationCommand
import com.yapp2app.map.application.command.GetPolygonLocationCommand
import com.yapp2app.map.application.port.PhotoBoothLocationRepositoryPort
import com.yapp2app.map.application.result.GetPointLocationResult
import com.yapp2app.map.application.result.GetPolygonLocationResult
import org.slf4j.LoggerFactory

/**
 * fileName       : GetPhotoBoothLocationUseCase
 * author         : darren
 * date           : 2026. 01. 17.
 * description    : 포토부스 위치 조회
 */
@UseCase
class GetPhotoBoothLocationUseCase(
    private val photoBoothLocationRepository: PhotoBoothLocationRepositoryPort,
    private val transactionRunner: TransactionRunner,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 다각형 기준으로 포토부스 위치 조회
     */
    fun execute(command: GetPolygonLocationCommand): GetPolygonLocationResult {
        // size + 1개 조회하여 hasNext 판단
        val fetchSize = command.size + 1

        val locations = transactionRunner.readOnly {
            photoBoothLocationRepository.listPolygonLocations(
                coordinates = command.coordinates,
                brandId = command.brandId,
                offset = command.page * command.size,
                limit = fetchSize,
            )
        }

        if (locations.isEmpty()) {
            return GetPolygonLocationResult(emptyList(), hasNext = false)
        }

        // hasNext 판단: size + 1개 조회했는데 실제로 그만큼 있으면 다음 페이지 존재
        val hasNext = locations.size > command.size

        val locationToReturn = if (hasNext) locations.dropLast(1) else locations

        return GetPolygonLocationResult(locationToReturn, hasNext)
    }

    /**
     * 특정 Point(사용자) 기준으로 포토부스 위치 조회
     */
    fun execute(command: GetPointLocationCommand): GetPointLocationResult {
        // size + 1개 조회하여 hasNext 판단
        val fetchSize = command.size + 1

        val locations = transactionRunner.readOnly {
            photoBoothLocationRepository.listPointLocations(
                longitude = command.longitude,
                latitude = command.latitude,
                radiusInMeters = command.radiusInMeters,
                brandId = command.brandId,
                offset = command.page * command.size,
                limit = fetchSize,
            )
        }

        if (locations.isEmpty()) {
            return GetPointLocationResult(emptyList(), hasNext = false)
        }

        // hasNext 판단: size + 1개 조회했는데 실제로 그만큼 있으면 다음 페이지 존재
        val hasNext = locations.size > command.size

        val locationToReturn = if (hasNext) locations.dropLast(1) else locations

        return GetPointLocationResult(locationToReturn, hasNext)
    }
}
