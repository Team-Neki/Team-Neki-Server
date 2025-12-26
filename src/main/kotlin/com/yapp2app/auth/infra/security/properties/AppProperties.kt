package com.yapp2app.auth.infra.security.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
class AppProperties(var auth: Auth = Auth(), var cors: Cors = Cors())

class Auth(var tokenSecret: String? = null, var tokenExpiry: Long = 0)

class Cors(var allowedOrigins: List<String> = emptyList())
