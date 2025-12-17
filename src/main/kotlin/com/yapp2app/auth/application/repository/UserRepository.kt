package com.yapp2app.auth.application.repository

import com.yapp2app.auth.domain.entity.User
import com.yapp2app.auth.domain.enums.ProviderType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmailAndProviderType(email: String, providerType: ProviderType): User?
}
