package pe.cevicherialourdes.shared.infrastructure.config;

import java.time.Clock;
import java.time.Duration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/** Firma y validacion de JWT con HS256 (Nimbus, via spring-boot-starter-oauth2-resource-server). */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    public static final String EMISOR = "cevicheria-lourdes";
    public static final String CLAIM_ROL = "rol";

    private static SecretKey clave(JwtProperties propiedades) {
        return new SecretKeySpec(propiedades.secretoEnBytes(), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties propiedades) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave(propiedades)));
    }

    /** Valida firma, algoritmo (solo HS256), expiracion (sin tolerancia, con el Clock de la app) y emisor. */
    @Bean
    public JwtDecoder jwtDecoder(JwtProperties propiedades, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave(propiedades))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtTimestampValidator vigencia = new JwtTimestampValidator(Duration.ZERO);
        vigencia.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(vigencia, new JwtIssuerValidator(EMISOR)));
        return decoder;
    }

    /** claim "rol": "ADMIN" -> autoridad ROLE_ADMIN. */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName(CLAIM_ROL);
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(autoridades);
        return converter;
    }
}
