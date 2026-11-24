package pe.cevicherialourdes.identidad.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.cevicherialourdes.identidad.domain.model.Rol;

interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {

    Optional<UsuarioEntity> findByUsername(String username);

    boolean existsByRol(Rol rol);
}
