package pe.cevicherialourdes.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import pe.cevicherialourdes.support.IntegracionBase;

/** HU-01 de punta a punta: arranque crea el ADMIN, login, bloqueo persistido, usuario desactivado. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.admin.username=mama",
        "app.admin.pin=2468"
})
class AutenticacionIT extends IntegracionBase {

    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;

    private ResponseEntity<Map> login(String username, String pin) {
        return http.postForEntity("/api/v1/auth/login", Map.of("username", username, "pin", pin), Map.class);
    }

    private void crearUsuario(String username, String pin, String rol, boolean activo) {
        jdbc.update("INSERT INTO usuario (nombre, username, pin_hash, rol, activo) VALUES (?, ?, ?, ?, ?)",
                "Prueba", username, encoder.encode(pin), rol, activo);
    }

    @Test
    void alArrancar_creaElPrimerAdminConElPinCifrado() {
        Map<String, Object> fila = jdbc.queryForMap("SELECT rol, pin_hash, activo FROM usuario WHERE username = 'mama'");
        assertThat(fila.get("rol")).isEqualTo("ADMIN");
        assertThat(fila.get("activo")).isEqualTo(true);
        assertThat((String) fila.get("pin_hash")).startsWith("$2").doesNotContain("2468");
    }

    @Test
    void loginCorrecto_devuelveUnTokenQueAbreRutasProtegidas() {
        ResponseEntity<Map> respuesta = login("mama", "2468");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsEntry("rol", "ADMIN").containsEntry("tipo", "Bearer");
        String token = (String) respuesta.getBody().get("token");

        // Sin token la API corta con 401; con token llega al dispatcher (404 de ruta inexistente)
        assertThat(http.getForEntity("/api/v1/no-existe", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        assertThat(http.exchange("/api/v1/no-existe", HttpMethod.GET, new HttpEntity<>(headers), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cincoFallosSeguidos_bloqueanAunqueLuegoElPinSeaCorrecto() {
        crearUsuario("hijo1", "1111", "MOZO", true);

        for (int i = 1; i <= 4; i++) {
            ResponseEntity<Map> r = login("hijo1", "0000");
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(r.getBody()).containsEntry("code", "CREDENCIALES_INVALIDAS");
        }
        ResponseEntity<Map> quinto = login("hijo1", "0000");
        assertThat(quinto.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
        assertThat(quinto.getBody()).containsEntry("code", "USUARIO_BLOQUEADO");

        ResponseEntity<Map> conPinCorrecto = login("hijo1", "1111");
        assertThat(conPinCorrecto.getStatusCode()).isEqualTo(HttpStatus.LOCKED);

        assertThat(jdbc.queryForObject(
                "SELECT bloqueado_hasta IS NOT NULL FROM usuario WHERE username = 'hijo1'", Boolean.class)).isTrue();
    }

    @Test
    void loginExitoso_reiniciaLosIntentosFallidos() {
        crearUsuario("hijo2", "2222", "MOZO", true);
        login("hijo2", "0000");
        login("hijo2", "0000");

        assertThat(login("hijo2", "2222").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jdbc.queryForObject(
                "SELECT intentos_fallidos FROM usuario WHERE username = 'hijo2'", Integer.class)).isZero();
    }

    @Test
    void usuarioDesactivado_noEntra() {
        crearUsuario("exmozo", "3333", "MOZO", false);

        ResponseEntity<Map> r = login("exmozo", "3333");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(r.getBody()).containsEntry("code", "CREDENCIALES_INVALIDAS");
    }

    @Test
    void elLoginApareceEnSwagger() {
        ResponseEntity<String> docs = http.getForEntity("/v3/api-docs", String.class);
        assertThat(docs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(docs.getBody()).contains("/api/v1/auth/login").contains("Rol: p");
    }
}
