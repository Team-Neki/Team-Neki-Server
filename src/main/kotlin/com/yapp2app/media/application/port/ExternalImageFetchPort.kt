package com.yapp2app.media.application.port

/**
 * fileName       : ExternalImageFetchPort
 * author         : koo
 * date           : 2026. 1. 28.
 * description    : 외부 URL에서 이미지를 다운로드하는 Port 인터페이스
 */
interface ExternalImageFetchPort {
    fun fetch(url: String): FetchResult?

    data class FetchResult(val binary: ByteArray, val contentType: String)
}
