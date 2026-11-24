package pe.cevicherialourdes.shared.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class DineroTest {

    @Test
    void siempreTieneEscala2ConRedondeoHalfUp() {
        assertThat(Dinero.de("15").monto()).isEqualTo(new BigDecimal("15.00"));
        assertThat(Dinero.de("2.005").monto()).isEqualTo(new BigDecimal("2.01"));
        assertThat(Dinero.de("2.004").monto()).isEqualTo(new BigDecimal("2.00"));
    }

    @Test
    void igualdadPorValorSinImportarLaEscalaDeEntrada() {
        assertThat(Dinero.de("15.0")).isEqualTo(Dinero.de(new BigDecimal("15.00")));
        assertThat(Dinero.de("15.0").hashCode()).isEqualTo(Dinero.de("15.00").hashCode());
    }

    @Test
    void sumaYMultiplica() {
        assertThat(Dinero.de("15.00").sumar(Dinero.de("1.00"))).isEqualTo(Dinero.de("16.00"));
        assertThat(Dinero.de("9.50").multiplicar(3)).isEqualTo(Dinero.de("28.50"));
        assertThat(Dinero.de("10.00").restar(Dinero.de("12.50"))).isEqualTo(Dinero.de("-2.50"));
    }

    @Test
    void comparaciones() {
        assertThat(Dinero.de("0.01").esPositivo()).isTrue();
        assertThat(Dinero.CERO.esPositivo()).isFalse();
        assertThat(Dinero.de("5").esMayorOIgualQue(Dinero.de("5.00"))).isTrue();
        assertThat(Dinero.de("4.99").esMayorOIgualQue(Dinero.de("5.00"))).isFalse();
    }

    @Test
    void noAceptaNulos() {
        assertThatThrownBy(() -> Dinero.de((BigDecimal) null)).isInstanceOf(NullPointerException.class);
    }
}
