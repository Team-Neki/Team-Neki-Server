package com.neki.domain.user.infra.security.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth")
class OauthProperties(var kakao: Kakao = Kakao(), var apple: Apple = Apple())

class Kakao(
    var androidClientId: String = "",
    var iosClientId: String = "",
    var clientSecret: String = "",
    var jwksUri: String = "",
    var issuer: String = "",
)

class Apple(var clientId: String = "", var jwksUri: String = "", var issuer: String = "")
