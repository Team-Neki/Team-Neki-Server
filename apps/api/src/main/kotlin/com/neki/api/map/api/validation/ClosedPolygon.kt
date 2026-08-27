package com.neki.api.map.api.validation

import com.neki.api.map.api.dto.MapRequest
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * fileName       : ClosedPolygon
 * author         : darren
 * date           : 2026. 8. 24.
 * description    : 다각형 좌표 리스트가 PostGIS 로 넘어가기 전에 걸러야 할 요청 제약을 모아둔다.
 */

/**
 * 좌표 리스트는 LINESTRING 문자열로 직렬화되어 SQL 에 그대로 들어간다.
 * 상한이 없으면 클라이언트가 보낸 좌표 개수만큼 쿼리 문자열이 무한정 커지므로 여기서 끊는다.
 * 좌표 1개당 약 23바이트이므로 1000개는 약 23KB 로, 지도 뷰포트/올가미 영역에는 충분히 넉넉하다.
 */
const val MAX_POLYGON_POINTS = 1000

const val MAX_POLYGON_POINTS_MESSAGE = "coordinates는 최대 ${MAX_POLYGON_POINTS}개까지 허용됩니다."

const val CLOSED_POLYGON_MESSAGE = "coordinates는 첫 좌표와 마지막 좌표가 동일한 4개 이상의 좌표여야 합니다."

/**
 * 좌표 리스트가 닫힌 링(4개 이상 + 첫 좌표 == 마지막 좌표)인지 검증한다.
 *
 * PostGIS ST_MakePolygon 은 shell 이 4개 미만이거나 닫히지 않으면 예외를 던지고,
 * 이는 전역 Exception 핸들러를 통해 500 으로 나간다. 잘못된 요청은 400 이어야 하므로 여기서 걸러낸다.
 *
 * 클래스 레벨이 아닌 필드 레벨 제약인 이유:
 * ExceptionHandler.methodValidExceptionHandler 가 ObjectError 를 FieldError 로 캐스팅하므로
 * 클래스 레벨 제약을 쓰면 ClassCastException 이 나면서 다시 500 이 된다.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ClosedPolygonValidator::class])
annotation class ClosedPolygon(
    val message: String = CLOSED_POLYGON_MESSAGE,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class ClosedPolygonValidator : ConstraintValidator<ClosedPolygon, List<MapRequest.GetPolygonLocation.Coordinate>> {

    override fun isValid(
        value: List<MapRequest.GetPolygonLocation.Coordinate>?,
        context: ConstraintValidatorContext,
    ): Boolean {
        // null·빈 리스트는 @NotEmpty, 좌표 내부 null 은 @Valid + @NotNull 이 각각 더 구체적인 메시지로 처리한다
        if (value.isNullOrEmpty()) return true
        if (value.any { it.longitude == null || it.latitude == null }) return true

        if (value.size < MIN_POLYGON_POINTS) return false

        val first: MapRequest.GetPolygonLocation.Coordinate = value.first()
        val last: MapRequest.GetPolygonLocation.Coordinate = value.last()

        return first.longitude == last.longitude && first.latitude == last.latitude
    }

    companion object {
        /** 닫힌 링을 만들려면 최소 3개의 꼭짓점 + 시작점 반복 1개가 필요하다. */
        private const val MIN_POLYGON_POINTS = 4
    }
}
