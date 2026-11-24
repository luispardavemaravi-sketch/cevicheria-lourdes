package pe.cevicherialourdes.identidad.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import pe.cevicherialourdes.identidad.domain.model.Rol;
import pe.cevicherialourdes.identidad.domain.model.Usuario;
import pe.cevicherialourdes.identidad.domain.port.out.UsuarioRepositorio;

@Component
class UsuarioPersistenceAdapter implements UsuarioRepositorio {

    private final UsuarioJpaRepository jpa;

    UsuarioPersistenceAdapter(UsuarioJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Usuario> buscarPorUsername(String username) {
        return jpa.findByUsername(username).map(UsuarioEntity::aDominio);
    }

    @Override
    public boolean existeConRol(Rol rol) {
        return jpa.existsByRol(rol);
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        return jpa.save(UsuarioEntity.desde(usuario)).aDominio();
    }
}
