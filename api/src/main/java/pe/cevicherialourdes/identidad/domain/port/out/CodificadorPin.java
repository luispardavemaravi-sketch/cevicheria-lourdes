package pe.cevicherialourdes.identidad.domain.port.out;

import pe.cevicherialourdes.identidad.domain.model.Pin;

public interface CodificadorPin {

    String codificar(Pin pin);

    boolean coincide(String pinEnClaro, String pinHash);

    /**
     * Gasta el mismo tiempo que {@link #coincide} contra un hash ficticio. Se usa cuando el usuario no existe
     * o esta desactivado, para que el tiempo de respuesta no revele cuales usernames existen.
     */
    void simularVerificacion(String pinEnClaro);
}
