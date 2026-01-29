package com.yapp2app.user.application.port

/**
 * fileName       : MediaClientPort
 * author         : koo
 * date           : 2026. 1. 28. 오후 3:59
 * description    :
 */
interface MediaClientPort {

    fun verifyMediaOwned(ownerId: Long, mediaId: Long)

    fun deleteMedia(ownerId: Long, mediaIds: Long)
}
