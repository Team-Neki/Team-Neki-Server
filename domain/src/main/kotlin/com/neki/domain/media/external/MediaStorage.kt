package com.neki.domain.media.external

import com.neki.domain.media.models.MediaRef
import com.neki.domain.media.models.MediaStorageUploadTicket

interface MediaStorage {

    fun deleteByKey(key: String)

    fun findByKey(key: String): String

    fun fetchBinaryByKey(key: String): ByteArray

    fun findAll(prefix: String): List<MediaRef>

    fun exists(key: String): Boolean

    fun generateUploadTicket(key: String, contentType: String): MediaStorageUploadTicket

    /**
     * 서버가 받은 바이트를 그대로 올린다. presigned URL 을 거치지 않는 관리자 일괄 업로드용이다.
     */
    fun put(key: String, contentType: String, content: ByteArray)
}
