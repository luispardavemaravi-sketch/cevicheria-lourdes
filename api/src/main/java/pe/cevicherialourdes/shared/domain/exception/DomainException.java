package pe.cevicherialourdes.shared.domain.exception;

import java.util.Objects;

/**
 * Raiz de las excepciones de negocio. El dominio no conoce HTTP: solo declara un codigo estable
 * (ej. MESA_OCUPADA) que el frontend puede usar, y la subclase indica la categoria del error.
 */
public abstract sealed class DomainException extends RuntimeException
        permits RecursoNoEncontradoException, ConflictoNegocioException, ReglaNegocioException {

    private final String codigo;

    protected DomainException(String codigo, String mensaje) {
        super(Objects.requireNonNull(mensaje, "mensaje"));
        if (codigo == null || !codigo.matches("[A-Z][A-Z0-9_]*")) {
            throw new IllegalArgumentException("El codigo debe ser MAYUSCULAS_CON_GUION_BAJO: " + codigo);
        }
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
