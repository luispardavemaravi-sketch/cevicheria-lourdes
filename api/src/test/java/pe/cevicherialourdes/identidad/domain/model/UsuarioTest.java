package pe.cevicherialourdes.identidad.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import pe.cevicherialourdes.shared.domain.exception.ReglaNegocioException;

class UsuarioTest {

    private static final Instant AHORA = Instant.parse("2026-11-24T13:00:00Z");

    private Usuario mozo() {
        return Usuario.nuevo("Hijo", "hijo", "$hash", Rol.MOZO);
    }

    private static void fallar(Usuario u, int veces, Instant cuando) {
        for (int i = 0; i < veces; i++) {
            u.registrarFallo(cuando);
        }
    }

    @Test
    void nuevo_normalizaElUsernameAMinusculasSinEspacios() {
        Usuario u = Usuario.nuevo("Mama", "  Mama  ", "$hash", Rol.ADMIN);
        assertThat(u.getUsername()).isEqualTo("mama");
        assertThat(u.isActivo()).isTrue();
        assertThat(u.getIntentosFallidos()).isZero();
        assertThat(u.getBloqueadoHasta()).isNull();
    }

    @Test
    void nuevo_rechazaUsernameConCaracteresInvalidos() {
        assertThatThrownBy(() -> Usuario.nuevo("X", "ma ma", "$hash", Rol.MOZO))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("USERNAME_INVALIDO");
    }

    @Test
    void nuevo_rechazaNombreVacio() {
        assertThatThrownBy(() -> Usuario.nuevo("  ", "hijo", "$hash", Rol.MOZO))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("NOMBRE_INVALIDO");
    }

    @Test
    void cuatroFallosNoBloquean() {
        Usuario u = mozo();
        fallar(u, 4, AHORA);
        assertThat(u.estaBloqueado(AHORA)).isFalse();
        assertThat(u.getIntentosFallidos()).isEqualTo(4);
    }

    @Test
    void elQuintoFalloSeguidoBloqueaCincoMinutos() {
        Usuario u = mozo();
        fallar(u, 5, AHORA);
        assertThat(u.estaBloqueado(AHORA)).isTrue();
        assertThat(u.estaBloqueado(AHORA.plus(Duration.ofMinutes(5)).minusSeconds(1))).isTrue();
        assertThat(u.estaBloqueado(AHORA.plus(Duration.ofMinutes(5)))).isFalse();
        assertThat(u.getBloqueadoHasta()).isEqualTo(AHORA.plus(Duration.ofMinutes(5)));
    }

    @Test
    void unAccesoExitosoReiniciaElConteo_losFallosDebenSerSeguidos() {
        Usuario u = mozo();
        fallar(u, 4, AHORA);
        u.registrarAccesoExitoso();
        u.registrarFallo(AHORA);
        assertThat(u.getIntentosFallidos()).isEqualTo(1);
        assertThat(u.estaBloqueado(AHORA)).isFalse();
    }

    @Test
    void trasVencerElBloqueo_seVuelveAContarDesdeCero() {
        Usuario u = mozo();
        fallar(u, 5, AHORA);
        Instant despues = AHORA.plus(Duration.ofMinutes(6));
        u.registrarFallo(despues);
        assertThat(u.estaBloqueado(despues)).isFalse();
        assertThat(u.getIntentosFallidos()).isEqualTo(1);
    }

    @Test
    void noSeRegistranFallosMientrasEstaBloqueado() {
        Usuario u = mozo();
        fallar(u, 5, AHORA);
        assertThatThrownBy(() -> u.registrarFallo(AHORA.plusSeconds(10)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void minutosRestantesDeBloqueo_redondeaHaciaArriba() {
        Usuario u = mozo();
        fallar(u, 5, AHORA);
        assertThat(u.minutosRestantesDeBloqueo(AHORA)).isEqualTo(5);
        assertThat(u.minutosRestantesDeBloqueo(AHORA.plusSeconds(61))).isEqualTo(4);
        assertThat(u.minutosRestantesDeBloqueo(AHORA.plusSeconds(299))).isEqualTo(1);
    }
}
