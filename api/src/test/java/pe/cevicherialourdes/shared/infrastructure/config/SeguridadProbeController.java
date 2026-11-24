package pe.cevicherialourdes.shared.infrastructure.config;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller solo de pruebas: un endpoint por rol para verificar JWT y la jerarquia de roles. */
@RestController
@RequestMapping("/test/seguridad")
public class SeguridadProbeController {

    @GetMapping("/autenticado")
    String autenticado() {
        return "ok";
    }

    @GetMapping("/mozo")
    @PreAuthorize("hasRole('MOZO')")
    String mozo() {
        return "ok";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    String admin() {
        return "ok";
    }

    @GetMapping("/cocina")
    @PreAuthorize("hasRole('COCINA')")
    String cocina() {
        return "ok";
    }
}
