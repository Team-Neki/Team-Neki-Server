package com.yapp2app

import org.assertj.core.api.Assertions.assertThat
import org.jasypt.encryption.StringEncryptor
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest
class JasyptTest(@Autowired private val jasyptStringEncryptor: StringEncryptor) {

    @Test
    fun jasyptGenerateTest() {
        val plain = "test_text"

        val encrypted = jasyptStringEncryptor.encrypt(plain)

        assertThat(encrypted).doesNotStartWith("ENC(")
        assertThat(jasyptStringEncryptor.decrypt(encrypted)).isEqualTo(plain)

        println("원본      : $plain")
        println("암호화    : ENC($encrypted)")
    }


    @Test
    fun jasyptDecryptTest() {
        val encText = "ENC(Tlk9CbdZwOMF7yO8va+hxDL6DAdNG8szceqMIazx69QgGDRNMkXVBgn8ZiMY2Bec)"

        assertThat(encText).startsWith("ENC(")

        val encrypted = encText.removePrefix("ENC(").removeSuffix(")")
        val decrypted = jasyptStringEncryptor.decrypt(encrypted)

        println("암호화    : $encText")
        println("복호화    : $decrypted")
    }
}
