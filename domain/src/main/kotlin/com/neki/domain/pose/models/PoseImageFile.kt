package com.neki.domain.pose.models

/**
 * fileName       : PoseImageFile
 * author         : darren
 * date           : 2026. 11. 9.
 * description    : 일괄 업로드로 받은 이미지 파일 하나. 업로드 형식(MultipartFile 등)을 domain 에 들이지 않으려고 감싼다.
 *                  바이트는 필요할 때 한 파일씩 읽는다.
 */
class PoseImageFile(val fileName: String, val readBytes: () -> ByteArray) {
    override fun toString(): String = "PoseImageFile(fileName=$fileName)"
}
