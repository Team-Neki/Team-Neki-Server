package com.neki.user.infra.persist

import com.neki.user.application.port.AppleUserTransferRepositoryPort
import com.neki.user.domain.entity.AppleUserTransfer
import com.neki.user.infra.persist.jpa.AppleUserTransferRepository
import org.springframework.stereotype.Repository

@Repository
class AppleUserTransferRepositoryAdapter(private val jpaRepository: AppleUserTransferRepository) :
    AppleUserTransferRepositoryPort {

    override fun save(appleUserTransfer: AppleUserTransfer): AppleUserTransfer = jpaRepository.save(appleUserTransfer)

    override fun findByNewSub(newSub: String): AppleUserTransfer? = jpaRepository.findByNewSub(newSub)
}
