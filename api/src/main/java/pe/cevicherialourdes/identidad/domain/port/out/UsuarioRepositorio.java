package pe.cevicherialourdes.identidad.domain.port.out;

import java.util.Optional;

import pe.cevicherialourdes.identidad.domain.model.Rol;
import pe.cevicherialourdes.identidad.domain.model.Usuario;

public interface UsuarioRepositorio {

    Optional<Usuario> buscarPorUsername(String username);

    boolean existeConRol(Rol rol);

    Usuario guardar(Usuario usuario);
}
