package pe.cevicherialourdes.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base de las pruebas *IT: un unico PostgreSQL 17 para toda la corrida (se arranca una vez y lo
 * reutilizan todas las clases que heredan). Cada prueba debe usar datos propios o limpiar lo que crea.
 */
public abstract class IntegracionBase {

    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }
}
