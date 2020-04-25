package io.github.t45k.clione

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ClioneApiController {
    @PostMapping("/event_handler")
    fun postEventHandler(obj: Any) {
        println(obj.toString())
    }
}
