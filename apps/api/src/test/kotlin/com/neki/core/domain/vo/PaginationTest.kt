package com.neki.core.domain.vo

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * fileName       : PaginationTest
 * description    : Pagination 유효성과 offset/limit/slice 단위 테스트
 */
class PaginationTest :
    FunSpec({

        test("offset 은 page * size 이고 limit 은 다음 페이지 확인용으로 한 건 더 조회한다") {
            val pagination = Pagination(page = 3, size = 20)

            pagination.offset shouldBe 60
            pagination.limit shouldBe 21
        }

        test("page * size 가 Int 최대값이면 아직 유효하다") {
            val pagination = Pagination(page = Int.MAX_VALUE, size = 1)

            pagination.offset shouldBe Int.MAX_VALUE
        }

        test("page * size 가 Int 를 넘으면 D-01") {
            val ex = shouldThrow<BusinessException> {
                Pagination(page = Int.MAX_VALUE, size = 100)
            }

            ex.resultCode shouldBe ResultCode.INVALID_PARAMETER
        }

        test("page 가 음수면 D-01") {
            val ex = shouldThrow<BusinessException> {
                Pagination(page = -1, size = 20)
            }

            ex.resultCode shouldBe ResultCode.INVALID_PARAMETER
        }

        test("size 가 1 미만이면 D-01") {
            val ex = shouldThrow<BusinessException> {
                Pagination(page = 0, size = 0)
            }

            ex.resultCode shouldBe ResultCode.INVALID_PARAMETER
        }

        test("size 를 넘겨 조회한 결과는 초과분을 잘라내고 hasNext 로 환산한다") {
            val page = Pagination(page = 0, size = 2).slice(listOf(1, 2, 3))

            page.items shouldBe listOf(1, 2)
            page.hasNext shouldBe true
        }

        test("size 이하로 조회되면 hasNext 는 false 다") {
            val page = Pagination(page = 0, size = 2).slice(listOf(1, 2))

            page.items shouldBe listOf(1, 2)
            page.hasNext shouldBe false
        }
    })
