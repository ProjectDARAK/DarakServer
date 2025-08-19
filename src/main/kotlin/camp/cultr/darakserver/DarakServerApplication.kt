package camp.cultr.darakserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties()
@ConfigurationPropertiesScan()
//@EnableSentry(dsn = BuildConfig.SENTRY_DSN, sendDefaultPii = false)
class DarakServerApplication

fun main(args: Array<String>) {
    runApplication<DarakServerApplication>(*args)
}
