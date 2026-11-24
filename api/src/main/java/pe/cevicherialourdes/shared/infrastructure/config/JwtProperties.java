package pe.cevicherialourdes.shared.infrastructure.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT_SECRET y duracion del token. Si el secreto falta o mide menos de 32 bytes (minimo de HS256),
 * la aplicacion NO arranca.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiracion) {

    public static final int MIN_BYTES_SECRETO = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_BYTES_SECRETO) {
            throw new IllegalStateException(
                    "JWT_SECRET es obligatorio y debe tener al menos " + MIN_BYTES_SECRETO + " bytes");
        }
        if (expiracion == null) {
            expiracion = Duration.ofHours(8);
        }
    }

    byte[] secretoEnBytes() {
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return "JwtProperties[secret=****, expiracion=" + expiracion + "]";
    }
}
