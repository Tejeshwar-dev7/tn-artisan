package in.artisanconnect.config;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/** Ensures Render can reach the embedded server on its externally scanned interface and port. */
@Configuration
@Profile("hosted")
public class HostedServerBinding {

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> renderTomcatBinding(
            @Value("${PORT:10000}") int port) {
        return factory -> {
            try {
                factory.setAddress(InetAddress.getByName("0.0.0.0"));
            } catch (UnknownHostException exception) {
                throw new IllegalStateException("Could not configure Tomcat for Render IPv4 traffic", exception);
            }
            factory.setPort(port);
        };
    }
}
