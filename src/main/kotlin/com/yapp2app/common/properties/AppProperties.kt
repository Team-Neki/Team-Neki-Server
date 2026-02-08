package com.yapp2app.common.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * fileName       : AppProperties
 * author         : koo
 * date           : 2026. 1. 21.
 * description    : 애플리케이션 전역 설정
 */
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
