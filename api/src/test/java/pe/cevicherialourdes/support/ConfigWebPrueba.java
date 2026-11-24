package pe.cevicherialourdes.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

import pe.cevicherialourdes.shared.infrastructure.config.ClockConfig;
import pe.cevicherialourdes.shared.infrastructure.config.ProblemDetailSecurityHandler;
import pe.cevicherialourdes.shared.infrastructure.config.SecurityConfig;

/** Lo minimo para un @WebMvcTest con la seguridad real (JWT, roles, ProblemDetail). */
@TestConfiguration
@Import({ClockConfig.class, SecurityConfig.class, ProblemDetailSecurityHandler.class})
public class ConfigWebPrueba {
}
