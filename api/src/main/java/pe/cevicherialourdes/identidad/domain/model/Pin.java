package pe.cevicherialourdes.identidad.domain.model;

import java.util.regex.Pattern;

import pe.cevicherialourdes.shared.domain.exception.ReglaNegocioException;

/** PIN de acceso en claro: exactamente 4 digitos ASCII. Solo vive en memoria hasta que se cifra o se compara. */
public record Pin(String valor) {

    private static final Pattern CUATRO_DIGITOS = Pattern.compile("[0-9]{4}");

    public Pin {
        if (valor == null || !CUATRO_DIGITOS.matcher(valor).matches()) {
            throw new ReglaNegocioException("PIN_INVALIDO", "El PIN debe tener exactamente 4 digitos");
        }
    }

    public static Pin de(String valor) {
        return new Pin(valor);
    }

    /** Nunca imprimir el PIN: ni en logs ni en mensajes de error. */
    @Override
    public String toString() {
        return "Pin[****]";
    }
}
