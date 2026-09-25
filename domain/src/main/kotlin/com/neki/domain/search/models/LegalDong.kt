package com.neki.domain.search.models

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable

/**
 * fileName       : LegalDong
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : 법정동. 행정표준코드 원본을 적재한 참조 테이블이라 애플리케이션에서 수정하지 않는다.
 *                  테이블은 데이터 적재 워크플로가 새 테이블을 만든 뒤 이름을 바꿔치기하는 식으로 관리한다.
 *                  이 저장소는 참조만 하므로 Flyway 마이그레이션을 두지 않고, 인덱스도 워크플로 쪽에서 건다.
 */
@Entity
@Immutable
@Table(name = "tb_legal_dong")
class LegalDong(
    /** 법정동코드 10자리 */
    @Id
    @Column(name = "code", nullable = false, columnDefinition = "char(10)")
    val code: String,

    /** 계층. 1 시도, 2 시군구, 3 읍면동, 4 리 */
    @Column(name = "level", nullable = false, columnDefinition = "smallint")
    val level: Int,

    /** 가장 아래 계층의 이름. 검색은 이 값으로만 한다. e.g. `강남구` */
    @Column(name = "leaf_name", nullable = false, length = 30)
    val leafName: String,

    /** 시도부터 이어 붙인 전체 경로. e.g. `서울특별시 강남구` */
    @Column(name = "full_name", nullable = false, length = 60)
    val fullName: String,
) {
    companion object {
        /** 시도는 검색 대상이 아니다. */
        const val SIDO_LEVEL: Int = 1
    }
}
