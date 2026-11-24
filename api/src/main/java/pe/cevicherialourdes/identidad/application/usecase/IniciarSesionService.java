package pe.cevicherialourdes.identidad.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.cevicherialourdes.identidad.domain.model.SesionIniciada;
import pe.cevicherialourdes.identidad.domain.model.TokenEmitido;
import pe.cevicherialourdes.identidad.domain.model.Usuario;
import pe.cevicherialourdes.identidad.domain.port.in.IniciarSesionUseCase;
import pe.cevicherialourdes.identidad.domain.port.out.CodificadorPin;
import pe.cevicherialourdes.identidad.domain.port.out.EmisorToken;
import pe.cevicherialourdes.identidad.domain.port.out.UsuarioRepositorio;
import pe.cevicherialourdes.shared.domain.exception.AccesoBloqueadoException;
import pe.cevicherialourdes.shared.domain.exception.NoAutenticadoException;

@Service
public class IniciarSesionService implements IniciarSesionUseCase {

    private static final Logger log = LoggerFactory.getLogger(IniciarSesionService.class);

    private final UsuarioRepositorio repositorio;
    private final CodificadorPin codificador;
    private final EmisorToken emisor;
    private final Clock clock;

    public IniciarSesionService(UsuarioRepositorio repositorio, CodificadorPin codificador, EmisorToken emisor,
                                Clock clock) {
        this.repositorio = repositorio;
        this.codificador = codificador;
        this.emisor = emisor;
        this.clock = clock;
    }

    /**
     * Las excepciones 401/423 NO deshacen la transaccion: el intento fallido y el bloqueo deben quedar
     * guardados aunque la respuesta sea un error.
     */
    @Override
    @Transactional(noRollbackFor = {NoAutenticadoException.class, AccesoBloqueadoException.class})
    public SesionIniciada iniciarSesion(String username, String pin) {
        Instant ahora = clock.instant();
        Optional<Usuario> encontrado = repositorio.buscarPorUsername(Usuario.claveDeBusqueda(username));

        if (encontrado.isEmpty() || !encontrado.get().isActivo()) {
            codificador.simularVerificacion(pin);
            log.info("Login rechazado: usuario inexistente o desactivado");
            throw credencialesInvalidas();
        }
        Usuario usuario = encontrado.get();

        if (usuario.estaBloqueado(ahora)) {
            log.info("Login rechazado: usuario {} bloqueado", usuario.getId());
            throw bloqueado(usuario, ahora);
        }

        if (!codificador.coincide(pin, usuario.getPinHash())) {
            usuario.registrarFallo(ahora);
            repositorio.guardar(usuario);
            if (usuario.estaBloqueado(ahora)) {
                log.warn("Usuario {} bloqueado {} min por {} PIN fallidos seguidos", usuario.getId(),
                        Usuario.DURACION_BLOQUEO.toMinutes(), Usuario.MAX_INTENTOS_FALLIDOS);
                throw bloqueado(usuario, ahora);
            }
            log.info("Login rechazado: PIN incorrecto para usuario {}", usuario.getId());
            throw credencialesInvalidas();
        }

        usuario.registrarAccesoExitoso();
        repositorio.guardar(usuario);
        TokenEmitido token = emisor.emitir(usuario);
        log.info("Login correcto: usuario {} ({})", usuario.getId(), usuario.getRol());
        return new SesionIniciada(token.valor(), token.expiraEn(), usuario.getId(), usuario.getNombre(),
                usuario.getRol());
    }

    private static NoAutenticadoException credencialesInvalidas() {
        return new NoAutenticadoException("CREDENCIALES_INVALIDAS", "Usuario o PIN incorrecto");
    }

    private static AccesoBloqueadoException bloqueado(Usuario usuario, Instant ahora) {
        long minutos = usuario.minutosRestantesDeBloqueo(ahora);
        return new AccesoBloqueadoException("USUARIO_BLOQUEADO",
                "Usuario bloqueado por intentos fallidos. Intenta de nuevo en " + minutos
                        + (minutos == 1 ? " minuto" : " minutos"));
    }
}
