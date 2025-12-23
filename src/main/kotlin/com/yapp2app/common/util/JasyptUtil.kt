package com.yapp2app.common.util

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig

/**
 * fileName       : JasyptUtil
 * author         : darren
 * date           : 2025. 12. 24.
 * description    : Jasypt 암호화/복호화 유틸리티
 *
 * 사용법:
 * 1. 암호화: JasyptUtil.encrypt("암호화할_값", "암호화_키")
 * 2. 복호화: JasyptUtil.decrypt("암호화된_값", "암호화_키")
 */
object JasyptUtil {

    private fun getEncryptor(password: String): PooledPBEStringEncryptor {
        val encryptor = PooledPBEStringEncryptor()
        val config = SimpleStringPBEConfig()

        config.password = password
        config.algorithm = "PBEWithMD5AndDES"
        config.setKeyObtentionIterations("1000")
        config.setPoolSize("1")
        config.providerName = "SunJCE"
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator")
        config.stringOutputType = "base64"

        encryptor.setConfig(config)
        return encryptor
    }

    fun encrypt(plainText: String, password: String): String {
        val encryptor = getEncryptor(password)
        return encryptor.encrypt(plainText)
    }

    fun decrypt(encryptedText: String, password: String): String {
        val encryptor = getEncryptor(password)
        return encryptor.decrypt(encryptedText)
    }
}
