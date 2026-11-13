package in.artisanconnect.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/** Creates the hosted datasource from Neon CLI's DATABASE_URL or Render DB_* settings. */
@Configuration
@Profile("hosted")
public class HostedDataSourceConfig {
    private static final String LOCAL_H2_URL = "jdbc:h2:file:./data/artisanconnect-v2;MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE";

    @Bean
    DataSource hostedDataSource(Environment environment) {
        Map<String, String> localEnv = readLocalEnvFile();
        String databaseUrl = value(environment, localEnv, "DATABASE_URL");
        String jdbcUrl;
        String username = value(environment, localEnv, "DB_USER");
        String password = value(environment, localEnv, "DB_PASSWORD");

        if (databaseUrl != null && !databaseUrl.isBlank()) {
            ConnectionParts parts = parseNeonUrl(databaseUrl);
            jdbcUrl = parts.jdbcUrl();
            username = parts.username();
            password = parts.password();
        } else {
            jdbcUrl = firstNonBlank(environment.getProperty("DB_URL"), environment.getProperty("spring.datasource.url"), LOCAL_H2_URL);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        if (username != null && !username.isBlank()) config.setUsername(username);
        if (password != null) config.setPassword(password);
        config.setMaximumPoolSize(5);
        config.setPoolName("tn-artisan-hosted");
        return new HikariDataSource(config);
    }

    private static ConnectionParts parseNeonUrl(String value) {
        URI uri = URI.create(value.strip());
        if (!"postgresql".equalsIgnoreCase(uri.getScheme()) && !"postgres".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("DATABASE_URL must be a PostgreSQL connection URI supplied by Neon.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) throw new IllegalArgumentException("DATABASE_URL is missing its database host.");
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String database = uri.getRawPath();
        if (database == null || database.length() < 2) throw new IllegalArgumentException("DATABASE_URL is missing its database name.");
        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + database
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        String[] credentials = uri.getRawUserInfo() == null ? new String[0] : uri.getRawUserInfo().split(":", 2);
        String username = credentials.length > 0 ? decode(credentials[0]) : null;
        String password = credentials.length > 1 ? decode(credentials[1]) : null;
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("DATABASE_URL must include the Neon database username and password.");
        }
        return new ConnectionParts(jdbcUrl, username, password);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String value(Environment environment, Map<String, String> localEnv, String key) {
        String value = environment.getProperty(key);
        return value == null || value.isBlank() ? localEnv.get(key) : value;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return LOCAL_H2_URL;
    }

    private static Map<String, String> readLocalEnvFile() {
        Path path = Path.of(".env.local");
        if (!Files.isRegularFile(path)) return Map.of();
        Map<String, String> values = new HashMap<>();
        try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
            lines.map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(line -> {
                        int separator = line.indexOf('=');
                        if (separator > 0) {
                            String key = line.substring(0, separator).strip();
                            String value = line.substring(separator + 1).strip();
                            if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                                    || (value.startsWith("'") && value.endsWith("'")))) {
                                value = value.substring(1, value.length() - 1);
                            }
                            values.put(key, value);
                        }
                    });
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read the local Neon environment file.", ex);
        }
        return values;
    }

    private record ConnectionParts(String jdbcUrl, String username, String password) { }
}
