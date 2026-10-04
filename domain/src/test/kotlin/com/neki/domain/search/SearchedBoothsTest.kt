package com.neki.domain.search

import com.neki.domain.search.models.BrandCount
import com.neki.domain.search.models.MapBooth
import com.neki.domain.search.models.PhotoBoothSearch
import com.neki.domain.search.models.SearchedBooths
import com.neki.domain.search.models.UserLocation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * fileName       : SearchedBoothsTest
 * author         : koo
 * date           : 2026. 10. 2.
 * description    : 색인 행과 지도 부스 짝짓기, 검색 정책 12장 정렬, 브랜드별 개수
 */
class SearchedBoothsTest :
    FunSpec({

        val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

        fun indexed(idx: String, brandId: Long, brandName: String, branchName: String, latitude: Double) =
            PhotoBoothSearch(
                platform = "P$brandId",
                idx = idx,
                brandId = brandId,
                brandName = brandName,
                brandCode = "B$brandId",
                branchName = branchName,
                address = null,
                location = geometryFactory.createPoint(Coordinate(127.0276, latitude)),
                normalizedBrandName = brandName,
                normalizedBranchName = branchName,
                searchText = brandName + branchName,
                regionIds = emptyArray(),
                siteKey = idx,
                sourceDt = LocalDate.of(2026, 10, 1),
                businessDate = LocalDate.of(2026, 10, 2),
                indexedAt = LocalDateTime.of(2026, 10, 2, 5, 30),
            )

        fun mapBooth(booth: PhotoBoothSearch, locationId: Long) =
            MapBooth(booth.platform, booth.idx, locationId, favorite = false)

        // 사용자 위치(위도 37.4979)에서 near 가 가장 가깝고 far 가 가장 멀다
        val near = indexed("a", 1, "포토이즘", "강남역점", 37.4980)
        val far = indexed("b", 1, "포토이즘", "가로수점", 37.5100)
        val sameSpotAsNear = indexed("c", 2, "인생네컷", "가나다점", 37.4980)
        val unsynced = indexed("d", 2, "인생네컷", "미동기화점", 37.4979)

        val booths: SearchedBooths = SearchedBooths.of(
            listOf(near, far, sameSpotAsNear, unsynced),
            listOf(mapBooth(near, 10), mapBooth(far, 11), mapBooth(sameSpotAsNear, 12)),
        )

        test("지도 부스와 원천 키가 같은 색인 행만 남는다") {
            booths.size shouldBe 3
        }

        test("사용자 위치가 있으면 가까운 순이고, 거리가 같으면 지점명 순이다") {
            booths.ordered(UserLocation(latitude = 37.4979, longitude = 127.0276)).map {
                it.mapBooth.locationId
            } shouldBe
                listOf(12L, 10L, 11L)
        }

        test("사용자 위치가 없으면 브랜드명, 지점명 순이다") {
            booths.ordered(null).map { it.mapBooth.locationId } shouldBe listOf(12L, 11L, 10L)
        }

        test("브랜드별로 세고 사용자 순서를 따르며, 순서에 없는 브랜드는 뒤에 id 순이다") {
            booths.countByBrand(orderedBrandIds = listOf(2L)) shouldBe listOf(
                BrandCount(brandId = 2, brandName = "인생네컷", brandCode = "B2", count = 1),
                BrandCount(brandId = 1, brandName = "포토이즘", brandCode = "B1", count = 2),
            )
            booths.countByBrand(orderedBrandIds = emptyList()).map { it.brandId } shouldBe listOf(1L, 2L)
        }
    })
