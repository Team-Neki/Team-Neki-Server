package com.yapp2app.common.api.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * fileName       : ObjectMapperConfig
 * author         : darren
 * date           : 2025. 12. 28. 23:43
 * description    : ObjectMapper Bean 등록
 */
@Configuration
class ObjectMapperConfig {

    companion object {
        private val DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    }

    @Bean
    fun objectMapper(): ObjectMapper {
        val javaTimeModule = JavaTimeModule().apply {
            addSerializer(Instant::class.java, InstantCustomSerializer())
            addSerializer(LocalDateTime::class.java, LocalDateTimeSerializer(DATE_TIME_FORMATTER))
        }

        return ObjectMapper()
            .registerKotlinModule()
            .registerModule(javaTimeModule)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    private class InstantCustomSerializer : JsonSerializer<Instant>() {
        override fun serialize(value: Instant, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeString(DATE_TIME_FORMATTER.format(value.atZone(ZoneOffset.UTC)))
        }
    }
}
