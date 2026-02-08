package com.yapp2app.common.api.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime

@DisplayName("ObjectMapperConfig 테스트")
class ObjectMapperConfigTest {

    private val objectMapperConfig = ObjectMapperConfig()
    private val objectMapper = objectMapperConfig.objectMapper()

    @Nested
    @DisplayName("Instant 직렬화")
    inner class InstantSerializationTest {

        @Test
        @DisplayName("Instant를 yyyy-MM-dd'T'HH:mm:ss 포맷으로 직렬화한다")
        fun givenInstant_whenSerialize_thenFormatsWithoutMillisAndZ() {
            // Given
            val instant = Instant.parse("2025-12-23T07:09:00.123456Z")
            val dto = InstantDto(instant)

            // When
            val json = objectMapper.writeValueAsString(dto)

            // Then
            assertThat(json).isEqualTo("""{"timestamp":"2025-12-23T07:09:00"}""")
        }

        @Test
        @DisplayName("Instant 직렬화 시 밀리초와 Z 접미사가 제거된다")
        fun givenInstantWithMillis_whenSerialize_thenRemovesMillisAndZ() {
            // Given
            val instant = Instant.parse("2026-01-24T15:30:45.999Z")
            val dto = InstantDto(instant)

            // When
            val json = objectMapper.writeValueAsString(dto)

            // Then
            assertThat(json).doesNotContain(".999")
            assertThat(json).doesNotContain("Z")
            assertThat(json).isEqualTo("""{"timestamp":"2026-01-24T15:30:45"}""")
        }
    }

    @Nested
    @DisplayName("LocalDateTime 직렬화")
    inner class LocalDateTimeSerializationTest {

        @Test
        @DisplayName("LocalDateTime을 yyyy-MM-dd'T'HH:mm:ss 포맷으로 직렬화한다")
        fun givenLocalDateTime_whenSerialize_thenFormatsCorrectly() {
            // Given
            val localDateTime = LocalDateTime.of(2025, 12, 23, 7, 9, 0)
            val dto = LocalDateTimeDto(localDateTime)

            // When
            val json = objectMapper.writeValueAsString(dto)

            // Then
            assertThat(json).isEqualTo("""{"createdAt":"2025-12-23T07:09:00"}""")
        }

        @Test
        @DisplayName("LocalDateTime 직렬화 시 나노초가 제거된다")
        fun givenLocalDateTimeWithNanos_whenSerialize_thenRemovesNanos() {
            // Given
            val localDateTime = LocalDateTime.of(2026, 1, 24, 15, 30, 45, 123456789)
            val dto = LocalDateTimeDto(localDateTime)

            // When
            val json = objectMapper.writeValueAsString(dto)

            // Then
            assertThat(json).doesNotContain(".123")
            assertThat(json).isEqualTo("""{"createdAt":"2026-01-24T15:30:45"}""")
        }
    }

    @Nested
    @DisplayName("Instant와 LocalDateTime 포맷 통일")
    inner class FormatConsistencyTest {

        @Test
        @DisplayName("Instant와 LocalDateTime이 동일한 포맷으로 직렬화된다")
        fun givenInstantAndLocalDateTime_whenSerialize_thenSameFormat() {
            // Given
            val instant = Instant.parse("2025-12-23T07:09:00Z")
            val localDateTime = LocalDateTime.of(2025, 12, 23, 7, 9, 0)
            val dto = CombinedDto(instant, localDateTime)

            // When
            val json = objectMapper.writeValueAsString(dto)

            // Then
            assertThat(json).isEqualTo(
                """{"expiresIn":"2025-12-23T07:09:00","createdAt":"2025-12-23T07:09:00"}""",
            )
        }
    }

    // Test DTOs
    private data class InstantDto(val timestamp: Instant)
    private data class LocalDateTimeDto(val createdAt: LocalDateTime)
    private data class CombinedDto(val expiresIn: Instant, val createdAt: LocalDateTime)
}
