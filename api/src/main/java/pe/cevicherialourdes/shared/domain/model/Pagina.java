package pe.cevicherialourdes.shared.domain.model;

import java.util.List;
import java.util.function.Function;

/** Resultado paginado, independiente de Spring Data (el dominio no conoce Page). */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos) {

    public Pagina {
        contenido = List.copyOf(contenido);
    }

    public int totalPaginas() {
        return tamano == 0 ? 0 : (int) ((totalElementos + tamano - 1) / tamano);
    }

    public <R> Pagina<R> mapear(Function<T, R> funcion) {
        return new Pagina<>(contenido.stream().map(funcion).toList(), pagina, tamano, totalElementos);
    }
}
