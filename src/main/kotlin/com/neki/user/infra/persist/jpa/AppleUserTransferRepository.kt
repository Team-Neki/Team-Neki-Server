package com.neki.user.infra.persist.jpa

import com.neki.user.domain.entity.AppleUserTransfer
import org.springframework.data.jpa.repository.JpaRepository

interface AppleUserTransferRepository : JpaRepository<AppleUserTransfer, Long> {
    fun findByNewSub(newSub: String): AppleUserTransfer?
}
