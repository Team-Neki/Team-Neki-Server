package com.yapp2app.common.config

import org.jasypt.encryption.StringEncryptor
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * fileName       : JasyptConfig
 * author         : darren
 * date           : 2025. 12. 24.
 * description    : Jasypt 암호화 설정
 */
@Configuration
class JasyptConfig {

    @Value("\${jasypt.encryptor.password}")
    private lateinit var encryptKey: String

    @Bean("jasyptStringEncryptor")
    fun stringEncryptor(): StringEncryptor {
        val encryptor = PooledPBEStringEncryptor()
        val config = SimpleStringPBEConfig()

        config.password = encryptKey
        config.algorithm = "PBEWithMD5AndDES"
        config.setKeyObtentionIterations("1000")
        config.setPoolSize("1")
        config.providerName = "SunJCE"
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator")
        config.stringOutputType = "base64"

        encryptor.setConfig(config)
        return encryptor
    }
}
