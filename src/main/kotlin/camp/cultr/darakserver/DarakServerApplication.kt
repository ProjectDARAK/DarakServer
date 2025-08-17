package camp.cultr.darakserver

import camp.cultr.darakserver.util.JwtProperties
import io.sentry.spring.jakarta.EnableSentry
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication

@EnableConfigurationProperties()
@ConfigurationPropertiesScan()
@EnableSentry(dsn = BuildConfig.SENTRY_DSN, sendDefaultPii = false)
class DarakServerApplication

fun main(args: Array<String>) {
    runApplication<DarakServerApplication>(*args)
}
