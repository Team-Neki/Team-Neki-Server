package com.neki.config.jasypt

import org.jasypt.encryption.StringEncryptor
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * fileName       : JasyptConfig
 * author         : darren
 * date           : 2025. 12. 24.
 * description    : Jasypt 암호화 설정
 */
@Profile("!test")
@Configuration
class JasyptConfig(
    @Value("\${jasypt.encryptor.password}")
    private val encryptKey: String,
) {

    @Bean("jasyptStringEncryptor")
    fun stringEncryptor(): StringEncryptor {
        val encryptor = PooledPBEStringEncryptor()
        val config = SimpleStringPBEConfig()

        config.password = encryptKey
        config.algorithm = "PBEWithHmacSHA512AndAES_256"
        config.setKeyObtentionIterations("1000")
        config.setPoolSize("1")
        config.providerName = "SunJCE"
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator")
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator")
        config.stringOutputType = "base64"

        encryptor.setConfig(config)
        return encryptor
    }
}
