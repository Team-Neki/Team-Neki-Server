package com.neki.user.infra.persist

import com.neki.user.domain.entity.AppleUserTransfer
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
@Import(AppleUserTransferRepositoryAdapter::class)
class AppleUserTransferRepositoryAdapterTest
@Autowired
constructor(
    private val adapter: AppleUserTransferRepositoryAdapter,
) {

    @Test
    fun `new_sub 로 매핑을 조회한다`() {
        // Given
        adapter.save(AppleUserTransfer(userId = 1L, oldSub = "A-sub", transferSub = "T-sub", newSub = "N-sub"))

        // When
        val found = adapter.findByNewSub("N-sub")

        // Then
        found!!.userId shouldBe 1L
        found.oldSub shouldBe "A-sub"
    }

    @Test
    fun `존재하지 않는 new_sub 는 null 을 반환한다`() {
        adapter.findByNewSub("none").shouldBeNull()
    }
}
