package com.yapp2app.common.api.dto

/**
 * fileName       : ResultCode
 * author         : darren
 * date           : 2025. 12. 12. 13:25
 * description    :
 */
enum class ResultCode(val code: String, val message: String) {

    SUCCESS("D-0", "OK"),

    ERROR("D-99", "일시적인 오류가 발생했어요."),
    INVALID_PARAMETER("D-01", "입력값이 올바르지 않습니다."),
    ALREADY_SIGNUP("D-02", "이미 회원가입된 계정입니다."),
    NOT_FOUND_USER("D-03", "가입된 계정이 없습니다."),
    NOT_FOUND("D-04", "데이터를 찾을 수 없습니다."),
    ALREADY_REQUEST("D-05", "이미 처리된 요청입니다."),
    UPLOAD_FAILED("D-06", "파일 업로드에 실패했습니다."),

    CONFLICT_FOLDER("D-06", message = "해당하는 폴더가 존재합니다."),

    ACCESS_DENIED_ERROR("D-996", "토큰 권한이 없습니다."),
    MISSING_TOKEN_ERROR("D-996", "토큰이 존재하지 않습니다."),
    EXPIRED_TOKEN_ERROR("D-997", "토큰이 만료되었습니다."),
    INVALID_TOKEN_ERROR("D-998", "토큰이 올바르지 않습니다."),
    SECURITY_ERROR("D-999", "인증에 실패하였습니다."),
}
