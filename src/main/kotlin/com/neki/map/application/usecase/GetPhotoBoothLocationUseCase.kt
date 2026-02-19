package com.neki.map.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.transaction.TransactionRunner
import com.neki.map.application.command.GetPointLocationCommand
import com.neki.map.application.command.GetPolygonLocationCommand
import com.neki.map.application.port.PhotoBoothLocationRepositoryPort
import com.neki.map.application.result.GetPointLocationResult
import com.neki.map.application.result.GetPolygonLocationResult
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
        val locations = transactionRunner.readOnly {
            photoBoothLocationRepository.listPolygonLocations(
                coordinates = command.coordinates,
                brandIds = command.brandIds,
            )
        }

        if (locations.isEmpty()) {
            return GetPolygonLocationResult(emptyList())
        }

        return GetPolygonLocationResult(locations)
    }

    /**
     * 특정 Point(사용자) 기준으로 포토부스 위치 조회
     */
    fun execute(command: GetPointLocationCommand): GetPointLocationResult {
        val locations = transactionRunner.readOnly {
            photoBoothLocationRepository.listPointLocations(
                coordinate = command.coordinate,
                radiusInMeters = command.radiusInMeters,
                brandIds = command.brandIds,
            )
        }

        return GetPointLocationResult(locations)
    }
}
