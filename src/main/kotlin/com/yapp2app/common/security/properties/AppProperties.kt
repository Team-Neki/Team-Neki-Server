package com.yapp2app.common.security.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
class AppProperties(var auth: Auth = Auth())

class Auth(var tokenSecret: String? = null, var tokenExpiry: Long = 0)
