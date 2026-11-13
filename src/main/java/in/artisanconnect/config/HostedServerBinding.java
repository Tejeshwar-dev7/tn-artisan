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
            // Only JSTL provides tag libraries. Scanning every dependency in the
            // executable WAR is expensive on a small hosted instance.
            factory.addContextCustomizers(context -> {
                org.apache.tomcat.util.scan.StandardJarScanner scanner = new org.apache.tomcat.util.scan.StandardJarScanner();
                org.apache.tomcat.util.scan.StandardJarScanFilter filter = new org.apache.tomcat.util.scan.StandardJarScanFilter();
                filter.setTldSkip("*");
                filter.setTldScan("jakarta.servlet.jsp.jstl-*.jar");
                scanner.setJarScanFilter(filter);
                scanner.setScanManifest(false);
                context.setJarScanner(scanner);
            });
        };
    }
}
