package com.yapp2app

import com.yapp2app.common.security.properties.AppProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AppProperties::class)
class YappApp2SeverApplication

fun main(args: Array<String>) {
    runApplication<YappApp2SeverApplication>(*args)
}
