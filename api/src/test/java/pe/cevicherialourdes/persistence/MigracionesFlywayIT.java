package pe.cevicherialourdes.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Levanta la aplicacion completa contra un PostgreSQL 17 real (Docker) y verifica que
 * Flyway cree el esquema, cargue los datos iniciales y que la base refuerce las reglas de negocio.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MigracionesFlywayIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TestRestTemplate http;

    @Test
    void flywayCreaLas15Tablas() {
        Integer tablas = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'
                """, Integer.class);
        assertThat(tablas).isEqualTo(15);
    }

    @Test
    void datosInicialesCarganMesasYCarta() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM mesa", Integer.class)).isEqualTo(14);
        assertThat(jdbc.queryForObject(
                "SELECT precio FROM producto WHERE nombre = 'Ceviche de pescado'", BigDecimal.class))
                .isEqualByComparingTo("15.00");
        assertThat(jdbc.queryForObject("""
                SELECT pr.precio FROM presentacion pr JOIN producto p ON p.id = pr.producto_id
                WHERE p.nombre = 'Inca Kola' AND pr.nombre = '1.5 L'
                """, BigDecimal.class))
                .isEqualByComparingTo("9.50");
    }

    @Test
    void rn13_laBaseRechazaDosCuentasAbiertasEnLaMismaMesa() {
        String abrirMesa3 = """
                INSERT INTO cuenta (tipo, mesa_numero, estado, abierta_en)
                VALUES ('MESA', 3, 'ABIERTA', now())
                """;
        jdbc.update(abrirMesa3);
        assertThatThrownBy(() -> jdbc.update(abrirMesa3))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void healthResponde200() {
        var respuesta = http.getForEntity("/actuator/health", String.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).contains("UP");
    }
}
