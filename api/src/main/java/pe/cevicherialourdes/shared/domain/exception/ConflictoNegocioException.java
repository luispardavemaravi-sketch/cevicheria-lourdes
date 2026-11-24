package pe.cevicherialourdes.shared.domain.exception;

/** El estado actual impide la operacion (se traduce a 409). Ej.: MESA_OCUPADA, CUENTA_YA_PAGADA. */
public final class ConflictoNegocioException extends DomainException {

    public ConflictoNegocioException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
