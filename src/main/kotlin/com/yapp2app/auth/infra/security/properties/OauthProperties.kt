package com.yapp2app.auth.infra.security.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth")
class OauthProperties(var kakao: Kakao = Kakao(), var apple: Apple = Apple())

class Kakao(var clientId: String = "", var clientSecret: String = "", var jwksUri: String = "", var issuer: String = "")

class Apple(var key: String = "")
