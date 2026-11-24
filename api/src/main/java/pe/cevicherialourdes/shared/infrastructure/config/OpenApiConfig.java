package pe.cevicherialourdes.shared.infrastructure.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Swagger: todas las operaciones piden JWT salvo las marcadas con @SecurityRequirements vacio.
 * Convencion: la descripcion de cada operacion empieza con "Rol: ..." (que roles pueden llamarla).
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Cevicheria Lourdes - Pedidos y Caja", version = "v1"),
        security = @SecurityRequirement(name = "bearer"))
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
