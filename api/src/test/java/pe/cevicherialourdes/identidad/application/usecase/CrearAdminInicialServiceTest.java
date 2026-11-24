package pe.cevicherialourdes.identidad.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.cevicherialourdes.identidad.domain.model.Pin;
import pe.cevicherialourdes.identidad.domain.model.Rol;
import pe.cevicherialourdes.identidad.domain.model.Usuario;
import pe.cevicherialourdes.identidad.domain.port.in.CrearAdminInicialUseCase.Resultado;
import pe.cevicherialourdes.identidad.domain.port.out.CodificadorPin;
import pe.cevicherialourdes.identidad.domain.port.out.UsuarioRepositorio;
import pe.cevicherialourdes.shared.domain.exception.ReglaNegocioException;

@ExtendWith(MockitoExtension.class)
class CrearAdminInicialServiceTest {

    @Mock UsuarioRepositorio repositorio;
    @Mock CodificadorPin codificador;
    @InjectMocks CrearAdminInicialService servicio;

    @Test
    void siYaExisteUnAdmin_noCreaNada() {
        when(repositorio.existeConRol(Rol.ADMIN)).thenReturn(true);

        assertThat(servicio.crearSiNoExiste("mama", "1234")).isEqualTo(Resultado.YA_EXISTE);
        verify(repositorio, never()).guardar(any());
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL,1234", "mama,NULL", "'',1234", "mama,'  '"}, nullValues = "NULL")
    void siFaltanVariables_noLoCrea(String username, String pin) {
        when(repositorio.existeConRol(Rol.ADMIN)).thenReturn(false);

        assertThat(servicio.crearSiNoExiste(username, pin)).isEqualTo(Resultado.FALTAN_DATOS);
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void creaElAdminConElPinCifrado() {
        when(repositorio.existeConRol(Rol.ADMIN)).thenReturn(false);
        when(codificador.codificar(Pin.de("1234"))).thenReturn("$2a$12$hash");

        assertThat(servicio.crearSiNoExiste("Mama", "1234")).isEqualTo(Resultado.CREADO);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repositorio).guardar(captor.capture());
        Usuario admin = captor.getValue();
        assertThat(admin.getUsername()).isEqualTo("mama");
        assertThat(admin.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(admin.getPinHash()).isEqualTo("$2a$12$hash");
        assertThat(admin.isActivo()).isTrue();
    }

    @Test
    void pinMalConfigurado_fallaEnVozAlta() {
        when(repositorio.existeConRol(Rol.ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> servicio.crearSiNoExiste("mama", "12"))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("PIN_INVALIDO");
        verify(repositorio, never()).guardar(any());
    }
}
