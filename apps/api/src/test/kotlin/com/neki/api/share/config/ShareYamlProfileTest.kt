package com.neki.api.share.config

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.PropertySource
import org.springframework.core.io.ClassPathResource

/**
 * application.yaml 의 프로필별 app.share.web-url-query 를 고정한다 (Spring 컨텍스트 없이 YAML 문서만 읽는다).
 * 공통 "" / local, staging "env=dev" / prod 는 share 키가 없어 공통 값("")을 쓴다.
 * YamlPropertySourceLoader 는 값을 문자열로만 읽고 ENC(...) 나 placeholder 를 해석하지 않는다.
 */
class ShareYamlProfileTest {

    private val documents: List<PropertySource<*>> =
        YamlPropertySourceLoader().load("application.yaml", ClassPathResource("application.yaml"))

    private fun document(profile: String?): PropertySource<*> = documents.single {
        it.getProperty(ON_PROFILE)?.toString() == profile
    }

    private fun webUrlQuery(profile: String?): Any? = document(profile).getProperty(WEB_URL_QUERY)

    @Test
    @DisplayName("공통 문서는 web-url 이 운영 웹이고 web-url-query 가 빈 값이다")
    fun `공통 값`() {
        document(null).getProperty("app.share.web-url").toString() shouldBe "https://neki-share.suitestudy.com"
        webUrlQuery(null).toString() shouldBe ""
    }

    @Test
    @DisplayName("local, staging 은 web-url-query 가 env=dev 다")
    fun `local staging 은 env=dev`() {
        webUrlQuery("local").toString() shouldBe "env=dev"
        webUrlQuery("staging").toString() shouldBe "env=dev"
    }

    @Test
    @DisplayName("prod 는 share 키를 덮어쓰지 않아 공통 값(쿼리 없음)을 쓴다")
    fun `prod 는 공통 값`() {
        val prod: PropertySource<*> = document("prod")
        prod.getProperty(WEB_URL_QUERY) shouldBe null
        prod.getProperty("app.share.web-url") shouldBe null
    }

    companion object {
        private const val ON_PROFILE = "spring.config.activate.on-profile"
        private const val WEB_URL_QUERY = "app.share.web-url-query"
    }
}
