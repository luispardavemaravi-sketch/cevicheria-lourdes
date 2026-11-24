package pe.cevicherialourdes.identidad.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.cevicherialourdes.identidad.domain.model.Pin;
import pe.cevicherialourdes.identidad.domain.model.Rol;
import pe.cevicherialourdes.identidad.domain.model.Usuario;
import pe.cevicherialourdes.identidad.domain.port.in.CrearAdminInicialUseCase;
import pe.cevicherialourdes.identidad.domain.port.out.CodificadorPin;
import pe.cevicherialourdes.identidad.domain.port.out.UsuarioRepositorio;

@Service
public class CrearAdminInicialService implements CrearAdminInicialUseCase {

    static final String NOMBRE_ADMIN_INICIAL = "Administrador";

    private final UsuarioRepositorio repositorio;
    private final CodificadorPin codificador;

    public CrearAdminInicialService(UsuarioRepositorio repositorio, CodificadorPin codificador) {
        this.repositorio = repositorio;
        this.codificador = codificador;
    }

    @Override
    @Transactional
    public Resultado crearSiNoExiste(String username, String pin) {
        if (repositorio.existeConRol(Rol.ADMIN)) {
            return Resultado.YA_EXISTE;
        }
        if (username == null || username.isBlank() || pin == null || pin.isBlank()) {
            return Resultado.FALTAN_DATOS;
        }
        String hash = codificador.codificar(Pin.de(pin));
        repositorio.guardar(Usuario.nuevo(NOMBRE_ADMIN_INICIAL, username, hash, Rol.ADMIN));
        return Resultado.CREADO;
    }
}
