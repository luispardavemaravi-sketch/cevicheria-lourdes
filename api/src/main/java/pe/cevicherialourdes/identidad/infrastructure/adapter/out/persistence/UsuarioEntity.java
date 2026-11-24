package pe.cevicherialourdes.identidad.infrastructure.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pe.cevicherialourdes.identidad.domain.model.Rol;
import pe.cevicherialourdes.identidad.domain.model.Usuario;

@Entity
@Table(name = "usuario")
class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 30, unique = true)
    private String username;

    @Column(name = "pin_hash", nullable = false, length = 72)
    private String pinHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "intentos_fallidos", nullable = false)
    private short intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    protected UsuarioEntity() {
    }

    static UsuarioEntity desde(Usuario u) {
        UsuarioEntity e = new UsuarioEntity();
        e.id = u.getId();
        e.nombre = u.getNombre();
        e.username = u.getUsername();
        e.pinHash = u.getPinHash();
        e.rol = u.getRol();
        e.activo = u.isActivo();
        e.intentosFallidos = (short) u.getIntentosFallidos();
        e.bloqueadoHasta = u.getBloqueadoHasta();
        return e;
    }

    Usuario aDominio() {
        return Usuario.reconstituir(id, nombre, username, pinHash, rol, activo, intentosFallidos, bloqueadoHasta);
    }
}
