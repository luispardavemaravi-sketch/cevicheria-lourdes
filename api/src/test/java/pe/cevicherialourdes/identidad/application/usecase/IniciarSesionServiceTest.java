package pe.cevicherialourdes.identidad.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.cevicherialourdes.identidad.domain.model.Rol;
import pe.cevicherialourdes.identidad.domain.model.SesionIniciada;
import pe.cevicherialourdes.identidad.domain.model.TokenEmitido;
import pe.cevicherialourdes.identidad.domain.model.Usuario;
import pe.cevicherialourdes.identidad.domain.port.out.CodificadorPin;
import pe.cevicherialourdes.identidad.domain.port.out.EmisorToken;
import pe.cevicherialourdes.identidad.domain.port.out.UsuarioRepositorio;
import pe.cevicherialourdes.shared.domain.exception.AccesoBloqueadoException;
import pe.cevicherialourdes.shared.domain.exception.NoAutenticadoException;

@ExtendWith(MockitoExtension.class)
class IniciarSesionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-24T13:00:00Z");

    @Mock UsuarioRepositorio repositorio;
    @Mock CodificadorPin codificador;
    @Mock EmisorToken emisor;

    IniciarSesionService servicio;

    @BeforeEach
    void setUp() {
        servicio = new IniciarSesionService(repositorio, codificador, emisor,
                Clock.fixed(AHORA, ZoneId.of("America/Lima")));
    }

    private static Usuario usuario(boolean activo, int intentos, Instant bloqueadoHasta) {
        return Usuario.reconstituir(7L, "Hijo", "hijo", "$hash", Rol.MOZO, activo, intentos, bloqueadoHasta);
    }

    @Test
    void pinCorrecto_emiteTokenYReiniciaIntentos() {
        Usuario u = usuario(true, 3, null);
        when(repositorio.buscarPorUsername("hijo")).thenReturn(Optional.of(u));
        when(codificador.coincide("1234", "$hash")).thenReturn(true);
        when(emisor.emitir(u)).thenReturn(new TokenEmitido("jwt", AHORA.plusSeconds(8 * 3600)));

        SesionIniciada sesion = servicio.iniciarSesion("  HIJO ", "1234");

        assertThat(sesion.token()).isEqualTo("jwt");
        assertThat(sesion.expiraEn()).isEqualTo(AHORA.plusSeconds(8 * 3600));
        assertThat(sesion.rol()).isEqualTo(Rol.MOZO);
        assertThat(sesion.usuarioId()).isEqualTo(7L);
        assertThat(u.getIntentosFallidos()).isZero();
        verify(repositorio).guardar(u);
    }

    @Test
    void usuarioInexistente_401YSimulaLaVerificacionParaNoRevelarloPorTiempo() {
        when(repositorio.buscarPorUsername("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciarSesion("nadie", "1234"))
                .isInstanceOf(NoAutenticadoException.class)
                .extracting("codigo").isEqualTo("CREDENCIALES_INVALIDAS");
        verify(codificador).simularVerificacion("1234");
        verify(emisor, never()).emitir(any());
    }

    @Test
    void usuarioDesactivado_noEntra() {
        when(repositorio.buscarPorUsername("hijo")).thenReturn(Optional.of(usuario(false, 0, null)));

        assertThatThrownBy(() -> servicio.iniciarSesion("hijo", "1234"))
                .isInstanceOf(NoAutenticadoException.class)
                .extracting("codigo").isEqualTo("CREDENCIALES_INVALIDAS");
        verify(codificador).simularVerificacion("1234");
        verify(codificador, never()).coincide(anyString(), anyString());
        verify(emisor, never()).emitir(any());
    }

    @Test
    void pinIncorrecto_401YGuardaElIntentoFallido() {
        when(repositorio.buscarPorUsername("hijo")).thenReturn(Optional.of(usuario(true, 0, null)));
        when(codificador.coincide("9999", "$hash")).thenReturn(false);

        assertThatThrownBy(() -> servicio.iniciarSesion("hijo", "9999"))
                .isInstanceOf(NoAutenticadoException.class)
                .extracting("codigo").isEqualTo("CREDENCIALES_INVALIDAS");

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(repositorio).guardar(guardado.capture());
        assertThat(guardado.getValue().getIntentosFallidos()).isEqualTo(1);
    }

    @Test
    void quintoPinIncorrecto_bloqueaYResponde423() {
        Usuario u = usuario(true, 4, null);
        when(repositorio.buscarPorUsername("hijo")).thenReturn(Optional.of(u));
        when(codificador.coincide("9999", "$hash")).thenReturn(false);

        assertThatThrownBy(() -> servicio.iniciarSesion("hijo", "9999"))
                .isInstanceOf(AccesoBloqueadoException.class)
                .extracting("codigo").isEqualTo("USUARIO_BLOQUEADO");
        assertThat(u.estaBloqueado(AHORA)).isTrue();
        verify(repositorio).guardar(u);
    }

    @Test
    void usuarioBloqueado_423SinVerificarElPinNiContarIntentos() {
        when(repositorio.buscarPorUsername("hijo"))
                .thenReturn(Optional.of(usuario(true, 0, AHORA.plusSeconds(120))));

        assertThatThrownBy(() -> servicio.iniciarSesion("hijo", "1234"))
                .isInstanceOf(AccesoBloqueadoException.class)
                .hasMessageContaining("2 minuto");
        verify(codificador, never()).coincide(anyString(), anyString());
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void bloqueoVencido_permiteEntrarConPinCorrecto() {
        Usuario u = usuario(true, 0, AHORA.minusSeconds(1));
        when(repositorio.buscarPorUsername("hijo")).thenReturn(Optional.of(u));
        when(codificador.coincide("1234", "$hash")).thenReturn(true);
        when(emisor.emitir(u)).thenReturn(new TokenEmitido("jwt", AHORA.plusSeconds(60)));

        assertThat(servicio.iniciarSesion("hijo", "1234").token()).isEqualTo("jwt");
        assertThat(u.getBloqueadoHasta()).isNull();
    }
}
