package com.neki.domain.user.models

/**
 * fileName       : TermAgreementStatus
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 약관 동의 현황. 어댑터가 support 도메인 응답을 변환해 넘겨준다.
 */
data class TermAgreementStatus(val requiredAgreed: Boolean, val marketingAgreed: Boolean)
