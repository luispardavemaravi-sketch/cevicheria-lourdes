package pe.cevicherialourdes.identidad.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** ADMIN_USERNAME / ADMIN_PIN. Solo se usan la primera vez, cuando no existe ningun ADMIN. */
@ConfigurationProperties(prefix = "app.admin")
public record AdminInicialProperties(String username, String pin) {

    @Override
    public String toString() {
        return "AdminInicialProperties[username=" + username + ", pin=****]";
    }
}
