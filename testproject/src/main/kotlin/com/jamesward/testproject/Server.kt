package com.jamesward.testproject

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.coRouter


@SpringBootApplication
class Server {
    @Bean
    fun http() = coRouter {
        GET("/") {
            ServerResponse.ok().bodyValueAndAwait("hello, world")
        }
    }
}

fun main(args: Array<String>) {
    runApplication<Server>(*args)
}
