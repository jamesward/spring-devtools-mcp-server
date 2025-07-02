package com.jamesward.testproject

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController


@SpringBootApplication
@RestController
class Server {

    @GetMapping
    fun index(): String = "hello, world"

}

fun main(args: Array<String>) {
    runApplication<Server>(*args)
}
