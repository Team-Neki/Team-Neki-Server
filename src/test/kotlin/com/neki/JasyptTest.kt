package com.neki

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
class JasyptTest {

    private val jasyptStringEncryptor = PooledPBEStringEncryptor().apply {
        setConfig(
            SimpleStringPBEConfig().apply {
                password = "testPasswordForJasypt"
                algorithm = "PBEWithHmacSHA512AndAES_256"
                setKeyObtentionIterations("1000")
                setPoolSize("1")
                providerName = "SunJCE"
                setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator")
                setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator")
                stringOutputType = "base64"
            },
        )
    }

    @Test
    fun jasyptGenerateTest() {
        println("=== Jasypt 암호화 유틸리티 ===")
        println()

        // 기타 암호화할 값
        val text = "test_text"
        val encryptedText = jasyptStringEncryptor.encrypt(text)
        println("   원본: $text")
        println("   암호화: ENC($encryptedText)")
        println("   복호화: ${jasyptStringEncryptor.decrypt(encryptedText)}")
        println()
    }
}
