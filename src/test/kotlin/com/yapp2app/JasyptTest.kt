package com.yapp2app

import org.jasypt.encryption.StringEncryptor
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class JasyptTest(@Autowired private val jasyptStringEncryptor: StringEncryptor) {

    @Test
    fun jasyptGeneratTest() {
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
