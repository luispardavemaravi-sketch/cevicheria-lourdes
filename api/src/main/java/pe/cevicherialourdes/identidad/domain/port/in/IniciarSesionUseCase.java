package pe.cevicherialourdes.identidad.domain.port.in;

import pe.cevicherialourdes.identidad.domain.model.SesionIniciada;

/** HU-01: login con usuario + PIN de 4 digitos. */
public interface IniciarSesionUseCase {

    /**
     * @throws pe.cevicherialourdes.shared.domain.exception.NoAutenticadoException CREDENCIALES_INVALIDAS
     * @throws pe.cevicherialourdes.shared.domain.exception.AccesoBloqueadoException USUARIO_BLOQUEADO
     */
    SesionIniciada iniciarSesion(String username, String pin);
}
