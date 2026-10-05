package com.neki.batch.notification.step

import com.neki.domain.notification.models.SendTarget
import org.springframework.batch.item.ItemReader

/**
 * keyset 페이징 ItemReader. 버퍼가 비고 소진이 아닌 동안 다음 페이지를 당겨 1건씩 흘린다.
 * 상태를 가지므로 @StepScope 빈으로만 쓴다 (실행마다 새 인스턴스). 단일 쓰레드 전제
 */
class PagingSendTargetItemReader(private val reader: SendTargetReader, private val pageSize: Int) :
    ItemReader<SendTarget> {

    private val buffer = ArrayDeque<SendTarget>()
    private var cursor = 0L
    private var exhausted = false

    override fun read(): SendTarget? {
        while (buffer.isEmpty() && !exhausted) {
            val page: SendTargetPage = reader.readPage(cursor, pageSize)
            buffer.addAll(page.targets)
            val next: Long? = page.nextCursor
            if (next == null) exhausted = true else cursor = next
        }
        return buffer.removeFirstOrNull()
    }
}
