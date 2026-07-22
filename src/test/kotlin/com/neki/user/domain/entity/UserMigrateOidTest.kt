package com.neki.user.domain.entity

import com.neki.testfixture.aUser
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class UserMigrateOidTest {

    @Test
    fun `migrateOid 는 oid 를 신규 값으로 교체한다`() {
        val user = aUser(oid = "old-A-sub")

        user.migrateOid("new-B-sub")

        user.oid shouldBe "new-B-sub"
    }
}
