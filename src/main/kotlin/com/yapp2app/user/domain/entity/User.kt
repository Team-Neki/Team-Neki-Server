package com.yapp2app.user.domain.entity

import com.yapp2app.common.domain.BaseTimeEntity
import com.yapp2app.user.domain.enums.ProviderType
import com.yapp2app.user.domain.enums.RoleType
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
    val email: String?,

    @Column(nullable = true)
    var password: String,

    @Column(nullable = false, length = 255)
    val oid: String,

    @Column(nullable = false, length = 100)
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

    fun changeProfileImage(newProfileImageId: Long) {
        this.profileImageId = newProfileImageId
    }

    fun changeName(newName: String) {
        this.name = newName
    }
}
