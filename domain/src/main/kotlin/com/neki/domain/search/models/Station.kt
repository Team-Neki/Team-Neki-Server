package com.neki.domain.search.models

/**
 * fileName       : Station
 * author         : koo
 * date           : 2026. 9. 15.
 * description    : 지하철역. 역명(`역` 접미사 없음)과 노선명. 한 역이 노선마다 따로 존재한다.
 */
data class Station(val name: String, val lineName: String)
