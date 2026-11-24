package pe.cevicherialourdes.shared.domain.exception;

/** Los datos son validos en forma pero violan una regla de negocio (se traduce a 422). Ej.: TAPER_SOLO_EN_MESA. */
public final class ReglaNegocioException extends DomainException {

    public ReglaNegocioException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
