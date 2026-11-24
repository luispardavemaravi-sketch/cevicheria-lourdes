package pe.cevicherialourdes.support;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base de las pruebas *IT: un unico PostgreSQL 17 para toda la corrida (se arranca una vez y lo
 * reutilizan todas las clases que heredan) y un unico contexto de Spring (mismas propiedades).
 * Cada prueba debe usar datos propios (nombres unicos) o limpiar lo que crea.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.admin.username=" + IntegracionBase.ADMIN_USERNAME,
        "app.admin.pin=" + IntegracionBase.ADMIN_PIN
})
public abstract class IntegracionBase {

    protected static final String ADMIN_USERNAME = "mama";
    protected static final String ADMIN_PIN = "2468";

    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired protected TestRestTemplate http;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected PasswordEncoder pinEncoder;

    /** Sufijo aleatorio corto para que los datos de una prueba no choquen con los de otra. */
    protected static String unico(String base) {
        return base + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    protected String tokenAdmin() {
        return token(ADMIN_USERNAME, ADMIN_PIN);
    }

    /** Crea un usuario activo del rol indicado y devuelve su token. */
    protected String tokenDe(String rol) {
        String username = "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO usuario (nombre, username, pin_hash, rol) VALUES (?, ?, ?, ?)",
                "Prueba " + rol, username, pinEncoder.encode("1357"), rol);
        return token(username, "1357");
    }

    @SuppressWarnings("rawtypes")
    protected String token(String username, String pin) {
        ResponseEntity<Map> r = http.postForEntity("/api/v1/auth/login",
                Map.of("username", username, "pin", pin), Map.class);
        return (String) r.getBody().get("token");
    }

    protected <T> ResponseEntity<T> llamar(HttpMethod metodo, String url, String token, Object cuerpo,
                                           Class<T> tipo) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return http.exchange(url, metodo, new HttpEntity<>(cuerpo, headers), tipo);
    }
}
