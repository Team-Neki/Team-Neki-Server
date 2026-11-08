package com.neki.domain.user.infra.security.session

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
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher

@Configuration
@ConditionalOnProperty(prefix = "admin.auth", name = ["enabled"], havingValue = "true")
@ComponentScan(basePackageClasses = [SessionOidcUserService::class])
@Import(UserRepositoryAdapter::class, AdminAccountService::class)
class SessionSecurityConfig {
    @Bean
    fun sessionSecurityFilterChain(
        http: HttpSecurity,
        oidcUsers: SessionOidcUserService,
        authorization: SessionAuthorizationManager,
        responses: SessionSecurityResponses,
    ): SecurityFilterChain = http
        // 브라우저는 Next 서버만 호출하고 Next가 서버 간 요청으로 중계한다. 다른 사이트의 변경 요청은
        // SameSite=Lax 세션 쿠키가 막으므로 CSRF 토큰을 쓰지 않는다. 대신 상태 변경은 GET으로 받지 않는다.
        .csrf { it.disable() }
        .authorizeHttpRequests {
            it.requestMatchers("/oauth2/authorization/*", "/login/oauth2/code/*").permitAll()
            // prod 는 springdoc 을 끄므로 이 경로는 404 다. local·staging 에서만 문서가 열린다.
            it.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
            it.requestMatchers(HttpMethod.GET, "/admin/auth/me").authenticated()
            it.requestMatchers(HttpMethod.POST, "/admin/auth/logout").authenticated()
            it.requestMatchers("/admin/**").access(authorization)
            it.anyRequest().denyAll()
        }
        .oauth2Login {
            it.userInfoEndpoint { endpoint -> endpoint.oidcUserService(oidcUsers) }
            // 내부 Service host가 브라우저로 노출되지 않도록 동일 origin의 상대 경로를 전달한다.
            it.successHandler { _, response, _ ->
                response.status = 302
                response.setHeader("Location", "/admin/auth/me")
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
            // CSRF를 끄면 logoutUrl은 GET도 받으므로 POST만 허용한다.
            it.logoutRequestMatcher(
                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/admin/auth/logout"),
            )
            it.invalidateHttpSession(true)
            it.clearAuthentication(true)
            it.deleteCookies("ADMINSESSION")
            it.logoutSuccessHandler(responses)
        }
        .build()
}
