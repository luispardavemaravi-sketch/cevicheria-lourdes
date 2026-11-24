package pe.cevicherialourdes.identidad.infrastructure.adapter.out.seguridad;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import pe.cevicherialourdes.identidad.domain.model.TokenEmitido;
import pe.cevicherialourdes.identidad.domain.model.Usuario;
import pe.cevicherialourdes.identidad.domain.port.out.EmisorToken;
import pe.cevicherialourdes.shared.infrastructure.config.JwtConfig;
import pe.cevicherialourdes.shared.infrastructure.config.JwtProperties;

/** JWT HS256. "sub" es el id del usuario (no el username ni el nombre: nada personal viaja en el token). */
@Component
class JwtEmisorToken implements EmisorToken {

    private final JwtEncoder encoder;
    private final JwtProperties propiedades;
    private final Clock clock;

    JwtEmisorToken(JwtEncoder encoder, JwtProperties propiedades, Clock clock) {
        this.encoder = encoder;
        this.propiedades = propiedades;
        this.clock = clock;
    }

    @Override
    public TokenEmitido emitir(Usuario usuario) {
        Instant ahora = clock.instant();
        Instant expira = ahora.plus(propiedades.expiracion());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.EMISOR)
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(ahora)
                .expiresAt(expira)
                .claim(JwtConfig.CLAIM_ROL, usuario.getRol().name())
                .build();
        String valor = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new TokenEmitido(valor, expira);
    }
}
