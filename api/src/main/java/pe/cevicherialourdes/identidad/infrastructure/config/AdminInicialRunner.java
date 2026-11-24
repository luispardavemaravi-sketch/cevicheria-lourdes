package pe.cevicherialourdes.identidad.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import pe.cevicherialourdes.identidad.domain.port.in.CrearAdminInicialUseCase;

/**
 * HU-01: al arrancar crea el primer ADMIN si no existe ninguno. Si faltan las variables, no lo crea y avisa
 * con WARN. Si el PIN esta mal configurado (no son 4 digitos) la aplicacion no arranca: mejor fallar en voz alta.
 */
@Component
@EnableConfigurationProperties(AdminInicialProperties.class)
class AdminInicialRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicialRunner.class);

    private final CrearAdminInicialUseCase crearAdmin;
    private final AdminInicialProperties propiedades;

    AdminInicialRunner(CrearAdminInicialUseCase crearAdmin, AdminInicialProperties propiedades) {
        this.crearAdmin = crearAdmin;
        this.propiedades = propiedades;
    }

    @Override
    public void run(ApplicationArguments args) {
        switch (crearAdmin.crearSiNoExiste(propiedades.username(), propiedades.pin())) {
            case CREADO -> log.info("Se creo el primer usuario ADMIN desde ADMIN_USERNAME/ADMIN_PIN");
            case FALTAN_DATOS -> log.warn("No existe ningun ADMIN y faltan ADMIN_USERNAME o ADMIN_PIN: "
                    + "no se crea el ADMIN inicial. Nadie podra administrar el sistema hasta definirlas.");
            case YA_EXISTE -> log.debug("Ya existe un ADMIN: no se crea el inicial");
        }
    }
}
