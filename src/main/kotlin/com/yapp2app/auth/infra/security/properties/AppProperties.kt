package com.yapp2app.auth.infra.security.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
class AppProperties(
    var version: String = "",
    var server: Server = Server(),
    var auth: Auth = Auth(),
    var cors: Cors = Cors(),
)

class Server(var url: String = "")

class Auth(
    var accessTokenSecret: String? = null,
    var accessTokenExpiry: Long = 0,
    var refreshTokenSecret: String? = null,
    var refreshTokenExpiry: Long = 0,
)

class Cors(var allowedOrigins: List<String> = emptyList())
