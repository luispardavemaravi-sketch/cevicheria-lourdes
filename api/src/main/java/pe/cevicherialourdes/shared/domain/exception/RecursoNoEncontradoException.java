package pe.cevicherialourdes.shared.domain.exception;

/** El recurso pedido no existe (se traduce a 404). Ej.: CUENTA_NO_ENCONTRADA. */
public final class RecursoNoEncontradoException extends DomainException {

    public RecursoNoEncontradoException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
