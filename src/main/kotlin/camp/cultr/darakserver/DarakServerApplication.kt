package camp.cultr.darakserver

import camp.cultr.darakserver.util.JwtProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(
    value = [JwtProperties::class]
)
class DarakServerApplication

fun main(args: Array<String>) {
    runApplication<DarakServerApplication>(*args)
}
