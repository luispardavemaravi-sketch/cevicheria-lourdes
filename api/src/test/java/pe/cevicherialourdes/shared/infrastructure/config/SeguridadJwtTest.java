package pe.cevicherialourdes.shared.infrastructure.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/** Valida el filtro JWT real (firma, expiracion, emisor) y RN-16: ADMIN hereda los permisos de MOZO. */
@WebMvcTest(controllers = SeguridadProbeController.class)
@Import({ClockConfig.class, SecurityConfig.class, ProblemDetailSecurityHandler.class})
class SeguridadJwtTest {

    @Autowired MockMvc mvc;
    @Autowired JwtEncoder encoder;

    private String token(String rol) {
        return token(encoder, rol, Instant.now().plusSeconds(3600), JwtConfig.EMISOR);
    }

    private static String token(JwtEncoder encoder, String rol, Instant expira, String emisor) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emisor)
                .subject("1")
                .issuedAt(expira.minusSeconds(7200))
                .expiresAt(expira)
                .claim(JwtConfig.CLAIM_ROL, rol)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Test
    void sinToken_401() throws Exception {
        mvc.perform(get("/test/seguridad/autenticado"))
           .andExpect(status().isUnauthorized())
           .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
    }

    @Test
    void tokenBasura_401() throws Exception {
        mvc.perform(get("/test/seguridad/autenticado").header("Authorization", "Bearer no.es.jwt"))
           .andExpect(status().isUnauthorized())
           .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
    }

    @Test
    void tokenVencido_401() throws Exception {
        String vencido = token(encoder, "ADMIN", Instant.now().minusSeconds(60), JwtConfig.EMISOR);
        mvc.perform(get("/test/seguridad/autenticado").header("Authorization", "Bearer " + vencido))
           .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenFirmadoConOtroSecreto_401() throws Exception {
        var otraClave = new SecretKeySpec(
                "otro-secreto-de-32-bytes-o-mas-para-hs256!".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        String falso = token(new NimbusJwtEncoder(new ImmutableSecret<>(otraClave)), "ADMIN",
                Instant.now().plusSeconds(3600), JwtConfig.EMISOR);
        mvc.perform(get("/test/seguridad/admin").header("Authorization", "Bearer " + falso))
           .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeOtroEmisor_401() throws Exception {
        String ajeno = token(encoder, "ADMIN", Instant.now().plusSeconds(3600), "otro-sistema");
        mvc.perform(get("/test/seguridad/admin").header("Authorization", "Bearer " + ajeno))
           .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenValido_pasa() throws Exception {
        mvc.perform(get("/test/seguridad/autenticado").header("Authorization", "Bearer " + token("COCINA")))
           .andExpect(status().isOk());
    }

    @Test
    void rn16_adminTieneLosPermisosDeMozo() throws Exception {
        mvc.perform(get("/test/seguridad/mozo").header("Authorization", "Bearer " + token("ADMIN")))
           .andExpect(status().isOk());
    }

    @Test
    void mozoNoTienePermisosDeAdmin_403() throws Exception {
        mvc.perform(get("/test/seguridad/admin").header("Authorization", "Bearer " + token("MOZO")))
           .andExpect(status().isForbidden())
           .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
    }

    @Test
    void cocinaNoEsMozo_403() throws Exception {
        mvc.perform(get("/test/seguridad/mozo").header("Authorization", "Bearer " + token("COCINA")))
           .andExpect(status().isForbidden());
    }

    @Test
    void adminNoHeredaCocina_403() throws Exception {
        mvc.perform(get("/test/seguridad/cocina").header("Authorization", "Bearer " + token("ADMIN")))
           .andExpect(status().isForbidden());
    }
}
