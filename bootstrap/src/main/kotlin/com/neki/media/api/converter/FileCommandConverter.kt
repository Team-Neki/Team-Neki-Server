package com.neki.media.api.converter

import com.neki.media.application.command.GetImageByKeyCommand
import org.springframework.stereotype.Component

/**
 * fileName       : FileCommandConverter
 * author         : koo
 * date           : 2026. 1. 21.
 * description    : File api layer command 변환을 위한 converter
 */
@Component
class FileCommandConverter {

    fun toGetImageByKeyCommand(objectKey: String): GetImageByKeyCommand = GetImageByKeyCommand(objectKey = objectKey)
}
