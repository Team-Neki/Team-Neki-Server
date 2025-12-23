package com.yapp2app.common.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth")
class OauthProperties(var kakao: Kakao = Kakao(), var apple: Apple = Apple())

class Kakao(var clientSecret: String = "")

class Apple(var key: String = "")
