package com.neki.api.share.config

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import java.time.Duration

class SharePropertiesTest {

    @Configuration
    @EnableConfigurationProperties(ShareProperties::class)
    class TestConfig

    private val contextRunner = ApplicationContextRunner().withUserConfiguration(TestConfig::class.java)

    @Test
    @DisplayName("app.share.* 를 바인딩하고 web-url-query 가 없으면 빈 값이다")
    fun `설정을 바인딩한다`() {
        contextRunner
            .withPropertyValues("app.share.web-url=https://share.example.com", "app.share.expiration=3d")
            .run { context ->
                context.startupFailure shouldBe null
                val properties: ShareProperties = context.getBean(ShareProperties::class.java)
                properties.webUrl shouldBe "https://share.example.com"
                properties.webUrlQuery shouldBe ""
                properties.expiration shouldBe Duration.ofDays(3)
                properties.cacheMaxAge shouldBe Duration.ofHours(1)
                properties.shareUrl("tok") shouldBe "https://share.example.com/s/tok"
            }
    }

    @Test
    @DisplayName("web-url-query 를 주면 공유 url 에 쿼리가 붙는다")
    fun `web-url-query 를 바인딩한다`() {
        contextRunner
            .withPropertyValues("app.share.web-url=https://share.example.com", "app.share.web-url-query=env=dev")
            .run { context ->
                val properties: ShareProperties = context.getBean(ShareProperties::class.java)
                properties.shareUrl("tok") shouldBe "https://share.example.com/s/tok?env=dev"
            }
    }

    @Test
    @DisplayName("web-url 이 비어 있으면 기동에 실패한다 (@Validated + @NotBlank)")
    fun `web-url 이 비면 기동 실패`() {
        contextRunner
            .withPropertyValues("app.share.web-url=  ")
            .run { context ->
                context.startupFailure shouldNotBe null
                // 바인딩 자체는 성공하고 Bean Validation 이 webUrl 을 거부해야 한다
                val trace: String = context.startupFailure!!.stackTraceToString()
                trace shouldContain "BindValidationException"
                trace shouldContain "webUrl"
            }
    }

    @Test
    @DisplayName("web-url 이 없으면 기동에 실패한다")
    fun `web-url 이 없으면 기동 실패`() {
        contextRunner.run { context -> context.startupFailure shouldNotBe null }
    }
}
