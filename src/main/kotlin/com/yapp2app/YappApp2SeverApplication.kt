package com.yapp2app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class YappApp2SeverApplication

fun main(args: Array<String>) {
    runApplication<YappApp2SeverApplication>(*args)
}
