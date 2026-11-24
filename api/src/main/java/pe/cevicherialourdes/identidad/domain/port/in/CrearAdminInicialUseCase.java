package pe.cevicherialourdes.identidad.domain.port.in;

/** HU-01: al arrancar, crea el primer ADMIN si todavia no existe ninguno. */
public interface CrearAdminInicialUseCase {

    enum Resultado { CREADO, YA_EXISTE, FALTAN_DATOS }

    /**
     * @param username puede ser null o vacio (variable de entorno sin definir)
     * @param pin      puede ser null o vacio; si viene, debe ser de 4 digitos (si no, PIN_INVALIDO)
     */
    Resultado crearSiNoExiste(String username, String pin);
}
