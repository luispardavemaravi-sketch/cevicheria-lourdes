package pe.cevicherialourdes.shared.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RN-14: el dia operativo se calcula en hora de Lima, aunque el servidor y la base de datos esten en UTC.
 * Todo el codigo obtiene la hora de este Clock (nunca LocalDate.now() sin argumento), asi las pruebas
 * pueden fijar la fecha.
 */
@Configuration
public class ClockConfig {

    public static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Lima");

    @Bean
    public Clock clock() {
        return Clock.system(ZONA_NEGOCIO);
    }
}
