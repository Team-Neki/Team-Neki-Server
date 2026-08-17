package com.neki.domain.user.models

import com.neki.core.domain.BaseTimeEntity
import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate

/**
 * fileName       : User
 * author         : darren
 * date           : 2025. 12. 18. 18:45
 * description    : User Entity
 */
@DynamicUpdate
@Entity
@Table(name = "TB_USERS")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var email: String?,

    @Column(nullable = true)
    var password: String,

    @Column(nullable = true, length = 255)
    var oid: String?,

    @Column(nullable = false, length = 10)
    var name: String?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val providerType: ProviderType,

    @Column(name = "profile_image_id", nullable = true)
    var profileImageId: Long?,

    @Column(name = "role", nullable = false, length = 255)
    var roles: String = RoleType.USER.role,
) : BaseTimeEntity() {
    constructor(
        email: String?,
        name: String?,
        oid: String,
        roles: String,
        providerType: ProviderType,
        profileImageId: Long?,
    ) : this(
        email = email ?: "NO_EMAIL",
        password = "NO_PASS",
        oid = oid,
        name = name,
        providerType = providerType,
        roles = roles,
        profileImageId = profileImageId,
    )

    fun updateName(newName: String) {
        this.name = newName
    }

    fun updateProfileImage(newProfileImageId: Long?) {
        this.profileImageId = newProfileImageId
    }

    fun withdraw() {
        this.email = null
        this.oid = null
    }
}
