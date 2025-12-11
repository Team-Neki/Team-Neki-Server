package com.yapp2app.common.security.token

import com.yapp2app.user.domain.entity.User
import com.yapp2app.user.domain.enums.ProviderType
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User

class UserPrincipal(
    @get:JvmName("name")
    val name: String,
    @get:JvmName("providerType")
    val providerType: ProviderType,
    @get:JvmName("email")
    val email: String,
    @get:JvmName("roles")
    val roles: Set<String>,
    @get:JvmName("getUserAttributes")
    val attributes: MutableMap<String, Any> = mutableMapOf<String, Any>(),
    @get:JvmName("password")
    private val password: String,
) : UserDetails,
    OidcUser,
    OAuth2User {

    /**
     * LOCAL 로그인 생길 시
     */
    constructor(user: User) : this(
        name = user.name,
        providerType = user.providerType,
        email = user.email,
        roles = user.roles.split(",").toSet(),
        password = user.password,
    )

    /**
     * OAuth 로그인 시
     */
    constructor(user: User, providerType: ProviderType, attributes: MutableMap<String, Any>) : this(
        name = user.name,
        providerType = providerType,
        email = user.email,
        roles = user.roles.split(",").toSet(),
        attributes = attributes,
        password = "NO_PASS",
    )

    override fun getName(): String = name

    override fun getAttributes(): MutableMap<String, Any> = attributes

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> = roles.map {
        GrantedAuthority { it.trim() }
    }.toMutableSet()

    override fun getClaims(): MutableMap<String, Any> = mutableMapOf<String, Any>()

    override fun getUserInfo(): OidcUserInfo? = null

    override fun getIdToken(): OidcIdToken? = null

    override fun getPassword(): String = password

    override fun getUsername(): String = email
}
