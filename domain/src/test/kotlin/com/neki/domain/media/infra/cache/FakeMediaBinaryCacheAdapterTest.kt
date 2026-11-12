package com.neki.domain.media.infra.cache

import com.neki.domain.media.infra.cache.fake.FakeMediaBinaryCacheAdapter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Duration

/**
 * staging 에서 쓰는 Fake 캐시가 메모리를 무한정 쓰지 않는지 검증
 */
class FakeMediaBinaryCacheAdapterTest :
    FunSpec({

        test("저장할 때 만료된 항목을 지운다 (다시 조회되지 않은 항목 포함)") {
            val adapter = FakeMediaBinaryCacheAdapter(maxBytes = 100)
            adapter.put("photo-booth/old.jpg", ByteArray(10), Duration.ofMillis(1))
            Thread.sleep(20)

            adapter.put("photo-booth/new.jpg", ByteArray(10), Duration.ofHours(1))

            adapter.storedBytes() shouldBe 10
            adapter.get("photo-booth/old.jpg") shouldBe null
        }

        test("상한을 넘으면 만료가 가장 가까운 항목부터 지운다") {
            val adapter = FakeMediaBinaryCacheAdapter(maxBytes = 100)
            adapter.put("photo-booth/soon.jpg", ByteArray(40), Duration.ofMinutes(1))
            adapter.put("photo-booth/later.jpg", ByteArray(40), Duration.ofHours(1))

            adapter.put("photo-booth/new.jpg", ByteArray(40), Duration.ofHours(1))

            adapter.get("photo-booth/soon.jpg") shouldBe null
            adapter.get("photo-booth/later.jpg") shouldNotBe null
            adapter.get("photo-booth/new.jpg") shouldNotBe null
            adapter.storedBytes() shouldBe 80
        }

        test("상한보다 큰 값은 저장하지 않는다") {
            val adapter = FakeMediaBinaryCacheAdapter(maxBytes = 100)

            adapter.put("photo-booth/huge.jpg", ByteArray(101), Duration.ofHours(1))

            adapter.get("photo-booth/huge.jpg") shouldBe null
            adapter.storedBytes() shouldBe 0
        }

        test("같은 key 를 다시 저장하거나 evict 하면 크기 합계가 맞게 줄어든다") {
            val adapter = FakeMediaBinaryCacheAdapter(maxBytes = 100)
            adapter.put("pose/a.jpg", ByteArray(30), Duration.ofHours(1))
            adapter.put("pose/a.jpg", ByteArray(20), Duration.ofHours(1))
            adapter.storedBytes() shouldBe 20

            adapter.evict("pose/a.jpg")

            adapter.storedBytes() shouldBe 0
            adapter.get("pose/a.jpg") shouldBe null
        }
    })
