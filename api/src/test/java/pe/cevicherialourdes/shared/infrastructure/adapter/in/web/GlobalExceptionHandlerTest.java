package pe.cevicherialourdes.shared.infrastructure.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import pe.cevicherialourdes.shared.infrastructure.config.ClockConfig;
import pe.cevicherialourdes.shared.infrastructure.config.ProblemDetailSecurityHandler;
import pe.cevicherialourdes.shared.infrastructure.config.SecurityConfig;

@WebMvcTest(controllers = ErrorProbeController.class)
@Import({ClockConfig.class, SecurityConfig.class, ProblemDetailSecurityHandler.class})
class GlobalExceptionHandlerTest {

    private static final String BASE = "/test/errores";

    @Autowired
    MockMvc mvc;

    @Test
    @WithMockUser
    void recursoNoEncontrado_devuelve404ConCodigoEstable() throws Exception {
        mvc.perform(get(BASE + "/no-encontrado"))
           .andExpect(status().isNotFound())
           .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
           .andExpect(jsonPath("$.code").value("CUENTA_NO_ENCONTRADA"))
           .andExpect(jsonPath("$.detail").value("La cuenta 152 no existe"))
           .andExpect(jsonPath("$.status").value(404))
           .andExpect(jsonPath("$.timestamp").exists())
           .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @WithMockUser
    void conflictoDeNegocio_devuelve409() throws Exception {
        mvc.perform(get(BASE + "/conflicto"))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.code").value("MESA_OCUPADA"));
    }

    @Test
    @WithMockUser
    void reglaDeNegocio_devuelve422() throws Exception {
        mvc.perform(get(BASE + "/regla"))
           .andExpect(status().isUnprocessableEntity())
           .andExpect(jsonPath("$.code").value("TAPER_SOLO_EN_MESA"));
    }

    @Test
    @WithMockUser
    void dtoInvalido_devuelve400ConLosCamposQueFallaron() throws Exception {
        mvc.perform(post(BASE + "/validacion")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre": "", "cantidad": -2}
                        """))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.code").value("VALIDACION_FALLIDA"))
           .andExpect(jsonPath("$.fields.length()").value(2))
           .andExpect(jsonPath("$.fields[?(@.campo == 'nombre')]").exists())
           .andExpect(jsonPath("$.fields[?(@.campo == 'cantidad')]").exists());
    }

    @Test
    @WithMockUser
    void jsonMalformado_devuelve400() throws Exception {
        mvc.perform(post(BASE + "/validacion")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ esto no es json"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.code").value("CUERPO_INVALIDO"));
    }

    @Test
    @WithMockUser
    void errorInesperado_devuelve500SinFiltrarDetallesInternos() throws Exception {
        mvc.perform(get(BASE + "/inesperado"))
           .andExpect(status().isInternalServerError())
           .andExpect(jsonPath("$.code").value("ERROR_INTERNO"))
           .andExpect(content().string(not(containsString("detalle interno"))))
           .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void sinSesion_devuelve401EnFormatoProblemDetail() throws Exception {
        mvc.perform(get(BASE + "/no-encontrado"))
           .andExpect(status().isUnauthorized())
           .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
           .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
    }
}
