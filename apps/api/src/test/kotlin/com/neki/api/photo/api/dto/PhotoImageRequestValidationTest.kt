package com.neki.api.photo.api.dto

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import jakarta.validation.Validation
import jakarta.validation.Validator

/**
 * fileName       : PhotoImageRequestValidationTest
 * description    : memo 길이는 PostgreSQL VARCHAR(255) 와 같이 code point 로 센다.
 * 테스트 DB(H2)는 UTF-16 단위로 세므로 E2E 대신 검증기만 확인한다.
 */
class PhotoImageRequestValidationTest :
    FunSpec({

        val validator: Validator = Validation.buildDefaultValidatorFactory().validator
        // UTF-16 으로는 2, code point 로는 1
        val emoji = "😀"

        test("서로게이트 쌍 이모지 255개 memo 는 통과한다") {
            validator.validate(PhotoImageRequest.UpdatePhoto(memo = emoji.repeat(255), capturedAt = null))
                .shouldBeEmpty()
        }

        test("code point 256개 memo 는 거절한다") {
            validator.validate(PhotoImageRequest.UpdatePhoto(memo = emoji.repeat(256), capturedAt = null))
                .shouldHaveSize(1)
        }
    })
