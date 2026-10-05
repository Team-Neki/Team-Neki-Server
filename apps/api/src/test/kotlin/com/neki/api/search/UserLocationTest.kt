package com.neki.api.search

import com.neki.domain.search.models.UserLocation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * fileName       : UserLocationTest
 * description    : UserLocation.distanceTo (haversine) 단위 테스트
 */
class UserLocationTest :
    FunSpec({

        // 강남역
        val gangnam = UserLocation(latitude = 37.4979, longitude = 127.0276)

        test("같은 좌표는 거리가 0 이다") {
            gangnam.distanceTo(gangnam.latitude, gangnam.longitude) shouldBe 0
        }

        test("강남역에서 포토이즘 강남1호점까지는 약 469m 이다") {
            gangnam.distanceTo(37.5021077, 127.0271830) shouldBe 469
        }

        test("위도 1도 차이는 약 111,195m 이다 (지구 둘레 / 360)") {
            UserLocation(0.0, 0.0).distanceTo(1.0, 0.0) shouldBe 111_195
        }

        test("거리는 대칭이다") {
            val booth = UserLocation(latitude = 37.5006179, longitude = 127.0253775)

            gangnam.distanceTo(booth.latitude, booth.longitude) shouldBe
                booth.distanceTo(gangnam.latitude, gangnam.longitude)
        }
    })
