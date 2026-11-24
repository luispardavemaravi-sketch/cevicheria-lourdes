package pe.cevicherialourdes.identidad.infrastructure.adapter.in.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import pe.cevicherialourdes.identidad.domain.port.in.IniciarSesionUseCase;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticacion")
public class AuthController {

    private final IniciarSesionUseCase iniciarSesion;
    private final LoginWebMapper mapper;

    public AuthController(IniciarSesionUseCase iniciarSesion, LoginWebMapper mapper) {
        this.iniciarSesion = iniciarSesion;
        this.mapper = mapper;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesion con usuario y PIN [HU-01]",
            description = "Rol: publico. Devuelve un JWT valido 8 horas. 5 PIN fallidos seguidos bloquean 5 minutos.")
    @ApiResponse(responseCode = "200", description = "Sesion iniciada")
    @ApiResponse(responseCode = "400", description = "VALIDACION_FALLIDA")
    @ApiResponse(responseCode = "401", description = "CREDENCIALES_INVALIDAS (incluye usuario desactivado)")
    @ApiResponse(responseCode = "423", description = "USUARIO_BLOQUEADO")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return mapper.aResponse(iniciarSesion.iniciarSesion(request.username(), request.pin()));
    }
}
