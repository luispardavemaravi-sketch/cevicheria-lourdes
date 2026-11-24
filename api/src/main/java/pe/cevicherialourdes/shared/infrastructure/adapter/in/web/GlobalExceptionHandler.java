package pe.cevicherialourdes.shared.infrastructure.adapter.in.web;

import java.net.URI;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import pe.cevicherialourdes.shared.domain.exception.ConflictoNegocioException;
import pe.cevicherialourdes.shared.domain.exception.DomainException;
import pe.cevicherialourdes.shared.domain.exception.RecursoNoEncontradoException;
import pe.cevicherialourdes.shared.domain.exception.ReglaNegocioException;

/**
 * Traduce toda excepcion a ProblemDetail (RFC 7807) con un "code" estable para el frontend.
 * Nunca expone stack traces, nombres de tablas ni mensajes internos.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    // ---- Negocio --------------------------------------------------------------------------

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<ProblemDetail> noEncontrado(RecursoNoEncontradoException ex) {
        return negocio(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(ConflictoNegocioException.class)
    ResponseEntity<ProblemDetail> conflicto(ConflictoNegocioException ex) {
        return negocio(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    ResponseEntity<ProblemDetail> reglaNegocio(ReglaNegocioException ex) {
        return negocio(HttpStatus.UNPROCESSABLE_ENTITY, ex);
    }

    // ---- Entrada invalida -----------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validacion(MethodArgumentNotValidException ex) {
        List<CampoInvalido> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new CampoInvalido(e.getField(), e.getDefaultMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "VALIDACION_FALLIDA",
                "Hay campos con datos invalidos", campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> cuerpoIlegible(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "CUERPO_INVALIDO",
                "El cuerpo de la solicitud no es un JSON valido", List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return responder(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO",
                "El parametro '" + ex.getName() + "' tiene un formato invalido", List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> rutaInexistente(NoResourceFoundException ex) {
        return responder(HttpStatus.NOT_FOUND, "RUTA_NO_ENCONTRADA", "La ruta solicitada no existe", List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> metodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NO_PERMITIDO",
                "Metodo HTTP no permitido en esta ruta", List.of());
    }

    // ---- Concurrencia y persistencia ------------------------------------------------------

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> concurrencia(ObjectOptimisticLockingFailureException ex) {
        log.warn("Conflicto de concurrencia: {}", ex.getPersistentClassName());
        return responder(HttpStatus.CONFLICT, "CONFLICTO_CONCURRENTE",
                "Otro usuario modifico este registro. Vuelve a cargar e intenta de nuevo", List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> integridad(DataIntegrityViolationException ex) {
        // El detalle (nombre de constraint) va al log, nunca al cliente.
        log.warn("Restriccion de base de datos violada: {}", ex.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, "RESTRICCION_VIOLADA",
                "La operacion viola una regla de datos del sistema", List.of());
    }

    // ---- Seguridad (cuando la lanza @PreAuthorize dentro del controller) -------------------

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> accesoDenegado(AccessDeniedException ex) {
        return responder(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO",
                "Tu rol no tiene permiso para esta operacion", List.of());
    }

    // ---- Ultima red ------------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> inesperado(Exception ex) {
        log.error("Error no controlado", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
                "Ocurrio un error inesperado. Intenta de nuevo", List.of());
    }

    // ---------------------------------------------------------------------------------------

    private ResponseEntity<ProblemDetail> negocio(HttpStatus status, DomainException ex) {
        return responder(status, ex.getCodigo(), ex.getMessage(), List.of());
    }

    private ResponseEntity<ProblemDetail> responder(HttpStatus status, String codigo, String detalle,
                                                    List<CampoInvalido> campos) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setType(URI.create("urn:cevicheria:error:" + codigo.toLowerCase(Locale.ROOT)));
        problema.setTitle(status.getReasonPhrase());
        problema.setProperty("code", codigo);
        problema.setProperty("timestamp", OffsetDateTime.now(clock));
        if (!campos.isEmpty()) {
            problema.setProperty("fields", campos);
        }
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problema);
    }

    public record CampoInvalido(String campo, String mensaje) {
    }
}
