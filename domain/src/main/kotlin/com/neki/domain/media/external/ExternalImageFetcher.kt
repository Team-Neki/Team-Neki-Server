package com.neki.domain.media.external

interface ExternalImageFetcher {
    fun fetch(url: String): FetchResult?

    data class FetchResult(val binary: ByteArray, val contentType: String)
}
