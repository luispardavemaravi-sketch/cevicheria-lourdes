package pe.cevicherialourdes.identidad.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import pe.cevicherialourdes.shared.domain.exception.ReglaNegocioException;

class PinTest {

    @Test
    void aceptaCuatroDigitos() {
        assertThat(Pin.de("0427").valor()).isEqualTo("0427");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123", "12345", "12a4", " 1234", "١٢٣٤"})
    void rechazaLoQueNoSeanExactamenteCuatroDigitosAscii(String valor) {
        assertThatThrownBy(() -> Pin.de(valor))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("PIN_INVALIDO");
    }

    @Test
    void toStringNuncaMuestraElPin() {
        assertThat(Pin.de("1234").toString()).doesNotContain("1234");
    }
}
