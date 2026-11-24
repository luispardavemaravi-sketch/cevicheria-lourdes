package pe.cevicherialourdes.identidad.infrastructure.adapter.out.seguridad;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import pe.cevicherialourdes.identidad.domain.model.Pin;
import pe.cevicherialourdes.identidad.domain.port.out.CodificadorPin;

@Component
class BCryptCodificadorPin implements CodificadorPin {

    private final PasswordEncoder encoder;
    /** Hash real (mismo costo) de un valor que nadie conoce; solo sirve para igualar tiempos. */
    private final String hashFicticio;

    BCryptCodificadorPin(PasswordEncoder encoder) {
        this.encoder = encoder;
        this.hashFicticio = encoder.encode("hash-ficticio-para-igualar-tiempos");
    }

    @Override
    public String codificar(Pin pin) {
        return encoder.encode(pin.valor());
    }

    @Override
    public boolean coincide(String pinEnClaro, String pinHash) {
        return encoder.matches(pinEnClaro, pinHash);
    }

    @Override
    public void simularVerificacion(String pinEnClaro) {
        encoder.matches(pinEnClaro, hashFicticio);
    }
}
