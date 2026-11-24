package pe.cevicherialourdes.identidad.domain.port.out;

import pe.cevicherialourdes.identidad.domain.model.TokenEmitido;
import pe.cevicherialourdes.identidad.domain.model.Usuario;

public interface EmisorToken {

    TokenEmitido emitir(Usuario usuario);
}
