package com.yapp2app.common.annotation

import org.springframework.stereotype.Component

/**
 * fileName       : UseCase
 * author         : koo
 * date           : 2025. 12. 23. 오후 6:51
 * description    : usecase annotation
 */
@Component
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class UseCase
