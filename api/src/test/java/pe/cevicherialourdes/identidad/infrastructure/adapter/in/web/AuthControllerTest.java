package pe.cevicherialourdes.identidad.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.cevicherialourdes.identidad.domain.model.Rol;
import pe.cevicherialourdes.identidad.domain.model.SesionIniciada;
import pe.cevicherialourdes.identidad.domain.port.in.IniciarSesionUseCase;
import pe.cevicherialourdes.shared.domain.exception.AccesoBloqueadoException;
import pe.cevicherialourdes.shared.domain.exception.NoAutenticadoException;
import pe.cevicherialourdes.shared.infrastructure.config.ClockConfig;
import pe.cevicherialourdes.shared.infrastructure.config.ProblemDetailSecurityHandler;
import pe.cevicherialourdes.shared.infrastructure.config.SecurityConfig;

@WebMvcTest(controllers = AuthController.class)
@Import({ClockConfig.class, SecurityConfig.class, ProblemDetailSecurityHandler.class, LoginWebMapperImpl.class})
class AuthControllerTest {

    private static final String URL = "/api/v1/auth/login";

    @Autowired MockMvc mvc;
    @MockitoBean IniciarSesionUseCase iniciarSesion;

    private static String cuerpo(String username, String pin) {
        return "{\"username\":\"" + username + "\",\"pin\":\"" + pin + "\"}";
    }

    @Test
    void loginCorrecto_esPublicoYDevuelveElToken() throws Exception {
        when(iniciarSesion.iniciarSesion("mama", "1234")).thenReturn(new SesionIniciada(
                "eyJ.token", Instant.parse("2026-11-24T21:00:00Z"), 1L, "Mama", Rol.ADMIN));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo("mama", "1234")))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.token").value("eyJ.token"))
           .andExpect(jsonPath("$.tipo").value("Bearer"))
           .andExpect(jsonPath("$.expiraEn").value("2026-11-24T21:00:00Z"))
           .andExpect(jsonPath("$.usuarioId").value(1))
           .andExpect(jsonPath("$.nombre").value("Mama"))
           .andExpect(jsonPath("$.rol").value("ADMIN"))
           .andExpect(jsonPath("$.pin").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "12345", "abcd", ""})
    void pinQueNoEsDeCuatroDigitos_400(String pin) throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo("mama", pin)))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.code").value("VALIDACION_FALLIDA"))
           .andExpect(jsonPath("$.fields[0].campo").value("pin"));
        verifyNoInteractions(iniciarSesion);
    }

    @Test
    void sinUsername_400() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"pin\":\"1234\"}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.fields[0].campo").value("username"));
    }

    @Test
    void usernameDemasiadoLargo_400() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo("x".repeat(31), "1234")))
           .andExpect(status().isBadRequest());
    }

    @Test
    void credencialesInvalidas_401() throws Exception {
        when(iniciarSesion.iniciarSesion(anyString(), anyString()))
                .thenThrow(new NoAutenticadoException("CREDENCIALES_INVALIDAS", "Usuario o PIN incorrecto"));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo("mama", "9999")))
           .andExpect(status().isUnauthorized())
           .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
           .andExpect(jsonPath("$.code").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void usuarioBloqueado_423() throws Exception {
        when(iniciarSesion.iniciarSesion(anyString(), anyString()))
                .thenThrow(new AccesoBloqueadoException("USUARIO_BLOQUEADO", "Intenta en 5 minutos"));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo("mama", "9999")))
           .andExpect(status().isLocked())
           .andExpect(jsonPath("$.code").value("USUARIO_BLOQUEADO"));
    }
}
