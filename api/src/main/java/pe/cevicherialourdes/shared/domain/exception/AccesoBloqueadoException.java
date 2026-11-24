package pe.cevicherialourdes.shared.domain.exception;

/** El recurso esta bloqueado temporalmente (se traduce a 423). Ej.: USUARIO_BLOQUEADO tras 5 PIN fallidos. */
public final class AccesoBloqueadoException extends DomainException {

    public AccesoBloqueadoException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
