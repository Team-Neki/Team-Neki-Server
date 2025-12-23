package com.yapp2app

import com.yapp2app.common.util.JasyptUtil
import org.junit.jupiter.api.Test

class JasyptTest {

    @Test
    fun jasyptGeneratTest() {
        val encryptKey = "" // 암호화 키 (환경변수로 관리 권장)

        println("=== Jasypt 암호화 유틸리티 ===")
        println()

        // 카카오 설정 암호화
        val text = "test_text"

        println("test 암호화:")
        val encryptedClientId = JasyptUtil.encrypt(text, encryptKey)
        println("   원본: $text")
        println("   암호화: $encryptedClientId")
        println("   복호화: ${JasyptUtil.decrypt(encryptedClientId, encryptKey)}")
        println()
    }
}
