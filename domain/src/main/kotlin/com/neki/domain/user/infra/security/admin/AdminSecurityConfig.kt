package com.neki.domain.user.infra.security.admin

import com.neki.domain.user.infra.persist.UserRepositoryAdapter
import com.neki.domain.user.service.AdminAccountService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.savedrequest.NullRequestCache

@Configuration
@ConditionalOnProperty(prefix = "admin.auth", name = ["enabled"], havingValue = "true")
@ComponentScan(basePackageClasses = [AdminOidcUserService::class])
@Import(UserRepositoryAdapter::class, AdminAccountService::class)
class AdminSecurityConfig {
    @Bean
    fun adminSecurityFilterChain(
        http: HttpSecurity,
        oidcUsers: AdminOidcUserService,
        oauth2Users: AdminOAuth2UserService,
        authorization: AdminAuthorizationManager,
        responses: AdminSecurityResponses,
    ): SecurityFilterChain = http
        .authorizeHttpRequests {
            it.requestMatchers("/oauth2/authorization/*", "/login/oauth2/code/*").permitAll()
            it.requestMatchers(HttpMethod.GET, "/admin/v1/auth/me").authenticated()
            it.requestMatchers(HttpMethod.POST, "/admin/v1/auth/logout").authenticated()
            it.requestMatchers("/admin/v1/**").access(authorization)
            it.anyRequest().denyAll()
        }
        .oauth2Login {
            it.userInfoEndpoint { endpoint ->
                endpoint.oidcUserService(oidcUsers)
                endpoint.userService(oauth2Users)
            }
            // 내부 Service host가 브라우저로 노출되지 않도록 동일 origin의 상대 경로를 전달한다.
            it.successHandler { _, response, _ ->
                response.status = 302
                response.setHeader("Location", "/admin/v1/auth/me")
            }
            it.failureHandler(responses)
        }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) }
        .requestCache { it.requestCache(NullRequestCache()) }
        .exceptionHandling {
            it.authenticationEntryPoint(responses)
            it.accessDeniedHandler(responses)
        }
        .logout {
            it.logoutUrl("/admin/v1/auth/logout")
            it.invalidateHttpSession(true)
            it.clearAuthentication(true)
            it.deleteCookies("ADMINSESSION")
            it.logoutSuccessHandler(responses)
        }
        .build()
}
