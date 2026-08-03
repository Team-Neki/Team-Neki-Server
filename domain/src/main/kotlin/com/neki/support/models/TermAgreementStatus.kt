package com.neki.support.models

/**
 * fileName       : TermAgreementStatus
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 필수/마케팅 약관 동의 현황 판정 결과
 */
data class TermAgreementStatus(val requiredAgreed: Boolean, val marketingAgreed: Boolean)
