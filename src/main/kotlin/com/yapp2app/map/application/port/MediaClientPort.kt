package com.yapp2app.map.application.port

import com.yapp2app.photo.application.contract.MediaStorageInfo

/**
 * fileName       : MediaClient
 * author         : darren
 * date           : 2026. 1. 22
 * description    : Media Client 호출을 위한 인터페이스
 */
interface MediaClientPort {

    fun getMediaStorageInfos(mediaIds: List<Long>): List<MediaStorageInfo>
}
