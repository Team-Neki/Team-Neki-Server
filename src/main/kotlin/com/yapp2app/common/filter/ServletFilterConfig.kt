package com.yapp2app.common.filter

import org.springframework.boot.autoconfigure.security.SecurityProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * fileName       : ServletFilterConfig
 * author         : koo
 * date           : 2026. 1. 8. 오후 3:44
 * description    :
 */
@Configuration
class ServletFilterConfig {

    @Bean
    fun requestMdcFilter(): RequestMdcFilter = RequestMdcFilter()

    @Bean
    fun requestMdcFilterRegistration(filter: RequestMdcFilter): FilterRegistrationBean<RequestMdcFilter> =
        FilterRegistrationBean(filter).apply {
            order = SecurityProperties.DEFAULT_FILTER_ORDER - 1
        }
}
