package pe.cevicherialourdes.shared.domain.model;

/** Pagina pedida (base 0). El tamano se acota a [1, 100]: ningun listado devuelve todo de golpe. */
public record Paginacion(int pagina, int tamano) {

    public static final int TAMANO_MAXIMO = 100;

    public Paginacion {
        pagina = Math.max(pagina, 0);
        tamano = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);
    }
}
