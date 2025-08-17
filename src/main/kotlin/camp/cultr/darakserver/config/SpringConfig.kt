package camp.cultr.darakserver.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.stereotype.Component

@Component class SpringConfig {}

@Component
class ServerFactoryCustomizer(@Value("\${darak.port}") private val port: Int) :
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
    override fun customize(factory: TomcatServletWebServerFactory) {
        factory.port = port
    }
}
