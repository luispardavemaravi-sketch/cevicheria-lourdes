package pe.cevicherialourdes.shared.domain.exception;

/** Las credenciales no identifican a un usuario habilitado (se traduce a 401). Ej.: CREDENCIALES_INVALIDAS. */
public final class NoAutenticadoException extends DomainException {

    public NoAutenticadoException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
