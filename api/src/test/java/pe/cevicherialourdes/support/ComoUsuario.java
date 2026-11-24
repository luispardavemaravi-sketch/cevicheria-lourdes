package pe.cevicherialourdes.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Peticiones de @WebMvcTest autenticadas con un JWT de cada rol (sub = id de usuario). */
public final class ComoUsuario {

    private ComoUsuario() {
    }

    public static RequestPostProcessor admin() {
        return conRol(1L, "ADMIN");
    }

    public static RequestPostProcessor mozo() {
        return conRol(2L, "MOZO");
    }

    public static RequestPostProcessor cocina() {
        return conRol(3L, "COCINA");
    }

    /**
     * Ojo: el post-procesador jwt() entrega las autoridades tal cual; la jerarquia ADMIN > MOZO la aplica
     * igual Spring Security al evaluar @PreAuthorize, por eso un admin() pasa por endpoints de MOZO.
     */
    public static RequestPostProcessor conRol(Long usuarioId, String rol) {
        return jwt().jwt(j -> j.subject(String.valueOf(usuarioId)).claim("rol", rol))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
