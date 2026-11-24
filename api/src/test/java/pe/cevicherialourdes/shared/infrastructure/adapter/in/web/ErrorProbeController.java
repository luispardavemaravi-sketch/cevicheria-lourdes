package pe.cevicherialourdes.shared.infrastructure.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import pe.cevicherialourdes.shared.domain.exception.AccesoBloqueadoException;
import pe.cevicherialourdes.shared.domain.exception.ConflictoNegocioException;
import pe.cevicherialourdes.shared.domain.exception.NoAutenticadoException;
import pe.cevicherialourdes.shared.domain.exception.RecursoNoEncontradoException;
import pe.cevicherialourdes.shared.domain.exception.ReglaNegocioException;

/** Controller solo de pruebas: provoca cada tipo de error para verificar el GlobalExceptionHandler. */
@RestController
@RequestMapping("/test/errores")
public class ErrorProbeController {

    @GetMapping("/no-encontrado")
    void noEncontrado() {
        throw new RecursoNoEncontradoException("CUENTA_NO_ENCONTRADA", "La cuenta 152 no existe");
    }

    @GetMapping("/conflicto")
    void conflicto() {
        throw new ConflictoNegocioException("MESA_OCUPADA", "La mesa 4 ya tiene una cuenta abierta");
    }

    @GetMapping("/regla")
    void regla() {
        throw new ReglaNegocioException("TAPER_SOLO_EN_MESA", "El taper solo se cobra en cuentas de mesa");
    }

    @GetMapping("/no-autenticado")
    void noAutenticado() {
        throw new NoAutenticadoException("CREDENCIALES_INVALIDAS", "Usuario o PIN incorrecto");
    }

    @GetMapping("/bloqueado")
    void bloqueado() {
        throw new AccesoBloqueadoException("USUARIO_BLOQUEADO", "Intenta de nuevo en 5 minutos");
    }

    @GetMapping("/inesperado")
    void inesperado() {
        throw new IllegalStateException("detalle interno que no debe salir");
    }

    @PostMapping("/validacion")
    void validacion(@Valid @RequestBody Solicitud solicitud) {
        // nunca llega: la validacion falla antes
    }

    public record Solicitud(@NotBlank String nombre, @Positive Integer cantidad) {
    }
}
