package com.neki.user.application.port

import com.neki.user.domain.entity.AppleUserTransfer

interface AppleUserTransferRepositoryPort {
    fun save(appleUserTransfer: AppleUserTransfer): AppleUserTransfer

    fun findByNewSub(newSub: String): AppleUserTransfer?
}
