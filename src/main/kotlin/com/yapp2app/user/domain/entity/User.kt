package com.yapp2app.user.domain.entity

import com.yapp2app.user.domain.enums.ProviderType
import com.yapp2app.user.domain.enums.RoleType
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = true)
    var password: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val providerType: ProviderType,

    @Column(name = "role", nullable = false, length = 255)
    var roles: String = RoleType.USER.role,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(nullable = false)
    var updatedAt: LocalDateTime? = null,
) {
    constructor(email: String, name: String, roles: String, providerType: ProviderType) : this(
        email = email,
        password = "NO_PASS",
        name = name,
        providerType = providerType,
        roles = roles,
    )
}
