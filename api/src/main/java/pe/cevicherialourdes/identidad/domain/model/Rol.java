package pe.cevicherialourdes.identidad.domain.model;

/** RN-16: ADMIN hereda los permisos de MOZO (la jerarquia se declara en SecurityConfig). */
public enum Rol {
    ADMIN,
    MOZO,
    COCINA
}
