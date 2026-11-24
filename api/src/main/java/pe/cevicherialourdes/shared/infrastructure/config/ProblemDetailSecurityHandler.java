package pe.cevicherialourdes.shared.infrastructure.config;

import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Los 401 y 403 que corta el filtro de seguridad no pasan por @RestControllerAdvice.
 * Este handler les da el mismo formato ProblemDetail que el resto de la API.
 */
@Component
public class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ProblemDetailSecurityHandler(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        escribir(response, HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO", "Debes iniciar sesion");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        escribir(response, HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", "Tu rol no tiene permiso para esta operacion");
    }

    private void escribir(HttpServletResponse response, HttpStatus status, String codigo, String detalle)
            throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setType(URI.create("urn:cevicheria:error:" + codigo.toLowerCase(java.util.Locale.ROOT)));
        problema.setTitle(status.getReasonPhrase());
        problema.setProperty("code", codigo);
        problema.setProperty("timestamp", OffsetDateTime.now(clock));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problema);
    }
}
