package camp.cultr.darakserver.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.stereotype.Component

@Component class SpringConfig {}

/**
 * Configures the server's port by customizing the TomcatServletWebServerFactory.
 *
 * This class implements WebServerFactoryCustomizer to allow programmatic customization
 * of the embedded Tomcat server's configuration. It uses the `darak.port` property value to
 * dynamically set the port on which the server will run.
 *
 * Dependencies:
 * - `darak.port`: A configuration property defining the desired server port.
 *
 * Key Responsibilities:
 * - Override the `customize` method of WebServerFactoryCustomizer.
 * - Set the server port using the loaded configuration.
 */
@Component
class ServerFactoryCustomizer(@Value("\${darak.port}") private val port: Int) :
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
    override fun customize(factory: TomcatServletWebServerFactory) {
        factory.port = port
    }
}
