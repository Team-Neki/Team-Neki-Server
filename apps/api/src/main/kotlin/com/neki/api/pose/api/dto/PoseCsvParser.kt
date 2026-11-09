package com.neki.api.pose.api.dto

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException

/**
 * fileName       : PoseCsvParser
 * author         : darren
 * date           : 2026. 11. 9.
 * description    : PM 일괄 업로드 CSV(pose.csv, media.csv) 입력 파서.
 *                  UTF-8, BOM 제거, CRLF/LF 모두 허용, 빈 줄은 건너뛴다. 헤더는 이름으로 찾으므로 열 순서는 자유다.
 *                  따옴표 인용은 지원하지 않는다. 형식이 틀리면 요청 전체를 INVALID_PARAMETER 로 거부한다.
 */
object PoseCsvParser {

    const val MAX_DATA_ROWS: Int = 1000

    private const val BOM: Char = '\uFEFF'
    private const val MEDIA_KEY = "media_key"
    private const val HEAD_COUNT = "head_count"
    private const val TAG = "tag"
    private const val FILE_NAME = "file_name"

    /**
     * @param lineNumber 파일의 물리 줄 번호 (헤더가 1)
     */
    data class PoseRow(val lineNumber: Int, val mediaKey: String, val headCount: String, val tag: String)

    fun parsePoseCsv(content: ByteArray): List<PoseRow> {
        val table: Table = parse(content, listOf(MEDIA_KEY, HEAD_COUNT, TAG))
        return table.rows.map { (lineNumber, fields) ->
            PoseRow(
                lineNumber = lineNumber,
                mediaKey = table.value(fields, MEDIA_KEY),
                headCount = table.value(fields, HEAD_COUNT),
                tag = table.value(fields, TAG),
            )
        }
    }

    /**
     * media_key → file_name. file_name 이 빈 행은 매핑하지 않고, 같은 키가 또 나오면 처음 것을 쓴다.
     */
    fun parseMediaCsv(content: ByteArray): Map<String, String> {
        val table: Table = parse(content, listOf(MEDIA_KEY, FILE_NAME))
        val mapping: LinkedHashMap<String, String> = LinkedHashMap()
        table.rows.forEach { (_, fields) ->
            val key: String = table.value(fields, MEDIA_KEY)
            val fileName: String = table.value(fields, FILE_NAME)
            if (key.isNotEmpty() && fileName.isNotEmpty()) {
                mapping.putIfAbsent(key, fileName)
            }
        }
        return mapping
    }

    private class Table(private val columnIndex: Map<String, Int>, val rows: List<Pair<Int, List<String>>>) {
        fun value(fields: List<String>, column: String): String =
            fields.getOrNull(columnIndex.getValue(column))?.trim().orEmpty()
    }

    private fun parse(content: ByteArray, requiredColumns: List<String>): Table {
        val text: String = content.toString(Charsets.UTF_8).removePrefix(BOM.toString())
        if (text.contains('"')) invalid()

        val lines: List<IndexedValue<String>> = text.split("\n")
            .map { it.removeSuffix("\r") }
            .withIndex()
            .filter { it.value.isNotBlank() }
        if (lines.isEmpty()) invalid()

        val header: List<String> = split(lines.first().value).map { it.trim().lowercase() }
        val columnIndex: Map<String, Int> = requiredColumns.associateWith { column ->
            header.indexOf(column).takeIf { it >= 0 } ?: invalid()
        }

        val rows: List<Pair<Int, List<String>>> = lines.drop(1).map { (index, line) -> (index + 1) to split(line) }
        if (rows.isEmpty() || rows.size > MAX_DATA_ROWS) invalid()

        return Table(columnIndex, rows)
    }

    private fun split(line: String): List<String> = line.split(",")

    private fun invalid(): Nothing = throw BusinessException(ResultCode.INVALID_PARAMETER)
}
