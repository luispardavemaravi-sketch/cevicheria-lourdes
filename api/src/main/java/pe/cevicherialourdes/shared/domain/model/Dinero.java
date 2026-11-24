package pe.cevicherialourdes.shared.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Monto en soles (PEN). Siempre BigDecimal con escala 2 y redondeo HALF_UP: nunca double/float.
 * Puede ser negativo (ej. diferencia de caja); cada regla de negocio decide si lo acepta.
 */
public record Dinero(BigDecimal monto) implements Comparable<Dinero> {

    public static final int ESCALA = 2;
    public static final RoundingMode REDONDEO = RoundingMode.HALF_UP;
    public static final Dinero CERO = new Dinero(BigDecimal.ZERO);

    public Dinero {
        monto = Objects.requireNonNull(monto, "monto").setScale(ESCALA, REDONDEO);
    }

    public static Dinero de(BigDecimal monto) {
        return new Dinero(monto);
    }

    public static Dinero de(String monto) {
        return new Dinero(new BigDecimal(monto));
    }

    public Dinero sumar(Dinero otro) {
        return new Dinero(monto.add(otro.monto));
    }

    public Dinero restar(Dinero otro) {
        return new Dinero(monto.subtract(otro.monto));
    }

    public Dinero multiplicar(int cantidad) {
        return new Dinero(monto.multiply(BigDecimal.valueOf(cantidad)));
    }

    public boolean esPositivo() {
        return monto.signum() > 0;
    }

    public boolean esMayorOIgualQue(Dinero otro) {
        return compareTo(otro) >= 0;
    }

    public boolean esMayorQue(Dinero otro) {
        return compareTo(otro) > 0;
    }

    @Override
    public int compareTo(Dinero otro) {
        return monto.compareTo(otro.monto);
    }

    @Override
    public String toString() {
        return "S/ " + monto.toPlainString();
    }
}
