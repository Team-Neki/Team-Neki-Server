package com.neki

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class NekiApplication

fun main(args: Array<String>) {
    runApplication<NekiApplication>(*args)
}
