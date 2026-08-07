package com.neki.media.external

import com.neki.media.models.MediaRef
import com.neki.media.models.MediaStorageUploadTicket

interface MediaStorage {

    fun deleteByKey(key: String)

    fun findByKey(key: String): String

    fun fetchBinaryByKey(key: String): ByteArray

    fun findAll(prefix: String): List<MediaRef>

    fun exists(key: String): Boolean

    fun generateUploadTicket(key: String, contentType: String): MediaStorageUploadTicket
}
