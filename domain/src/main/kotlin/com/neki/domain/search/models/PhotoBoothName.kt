package com.neki.domain.search.models

/**
 * fileName       : PhotoBoothName
 * author         : darren
 * date           : 2026. 10. 1.
 * description    : 부스 검색 자동완성에 쓰는 포토부스 이름. map 도메인의 포토부스를 SearchMapClient 가 옮겨 담는다.
 */
data class PhotoBoothName(val brandName: String, val branchName: String)
