package camp.cultr.darakserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DarakServerApplication

fun main(args: Array<String>) {
    runApplication<DarakServerApplication>(*args)
}
