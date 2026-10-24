package com.neki.domain.map.models

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

/**
 * fileName       : PhotoBoothKey
 * author         : darren
 * date           : 2026. 10. 13.
 * description    : 포토부스 고유 키. tb_photo_booth_enriched 의 (platform, idx) 이고 platform 은 브랜드 코드(tb_brand.code, V38)다.
 *   enriched 는 Workflow 가 매일 이름 교체로 새 세대를 만들어 행 id 가 없으므로, 세대를 넘어 안정적인 이 원천 키가 부스 식별자다.
 *   지점 마스터(TB_PHOTO_BOOTH_LOCATION)의 (source_platform, source_idx) 와 같은 값이다.
 */
@Embeddable
data class PhotoBoothKey(
    @Column(name = "platform", nullable = false)
    val platform: String,

    @Column(name = "idx", nullable = false)
    val idx: String,
) : Serializable {

    /** 앱에 내리는 부스 id. 지점 마스터 map_id 와 같은 `source:<platform>:<idx>` 모양이다 */
    fun toBoothId(): String = "$BOOTH_ID_PREFIX$platform$BOOTH_ID_DELIMITER$idx"

    companion object {
        private const val BOOTH_ID_PREFIX = "source:"
        private const val BOOTH_ID_DELIMITER = ":"

        // 지점 마스터 source_platform, source_idx 와 같은 길이 (V34)
        private const val MAX_PLATFORM_LENGTH = 32
        private const val MAX_IDX_LENGTH = 64

        /**
         * 부스 id 를 키로 되돌린다. 모양이 맞지 않으면 null.
         * platform(브랜드 코드)에는 `:` 가 없으므로 첫 `:` 뒤는 전부 idx 로 본다.
         */
        fun fromBoothId(boothId: String): PhotoBoothKey? {
            if (!boothId.startsWith(BOOTH_ID_PREFIX)) return null

            val parts: List<String> = boothId.removePrefix(BOOTH_ID_PREFIX).split(BOOTH_ID_DELIMITER, limit = 2)
            if (parts.size != 2) return null

            val (platform, idx) = parts
            if (platform.isBlank() || platform.length > MAX_PLATFORM_LENGTH) return null
            if (idx.isBlank() || idx.length > MAX_IDX_LENGTH) return null

            return PhotoBoothKey(platform = platform, idx = idx)
        }
    }
}
