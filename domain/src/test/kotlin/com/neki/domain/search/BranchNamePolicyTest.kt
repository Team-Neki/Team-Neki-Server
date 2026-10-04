package com.neki.domain.search

import com.neki.domain.search.service.qu.BranchNamePolicy
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * fileName       : BranchNamePolicyTest
 * author         : koo
 * date           : 2026. 9. 19.
 * description    : 지점 접미사(점, 본점, N호점) 판정
 */
class BranchNamePolicyTest :
    FunSpec({

        test("접미사로 시작하는지") {
            listOf("점", "본점", "2호점", "12호점", "점신촌").map(BranchNamePolicy::startsWithSuffix) shouldBe
                listOf(true, true, true, true, true)
            listOf("", "역점", "호점", "포토", "2호").map(BranchNamePolicy::startsWithSuffix) shouldBe
                listOf(false, false, false, false, false)
        }

        test("접미사로 끝나는 지점명인지") {
            listOf("강남점", "강남본점", "강남2호점", "라이브러리강남점").map(BranchNamePolicy::isBranchTerm) shouldBe
                listOf(true, true, true, true)
            listOf("강남", "라이브러리", "점프샵").map(BranchNamePolicy::isBranchTerm) shouldBe listOf(false, false, false)
        }
    })
