package pe.cevicherialourdes.carta.domain.model;

public enum Categoria {
    PLATO(true),
    BEBIDA(false),
    TAPER(false);

    /** RN-06: bebidas y taper no pasan por la cola de cocina, salvo que se indique otra cosa. */
    private final boolean requiereCocinaPorDefecto;

    Categoria(boolean requiereCocinaPorDefecto) {
        this.requiereCocinaPorDefecto = requiereCocinaPorDefecto;
    }

    public boolean requiereCocinaPorDefecto() {
        return requiereCocinaPorDefecto;
    }

    /** Las bebidas tienen el precio en cada presentacion (HU-04); el resto, en el producto. */
    public boolean llevaPrecioPropio() {
        return this != BEBIDA;
    }
}
