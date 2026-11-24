package pe.cevicherialourdes.shared.infrastructure.adapter.in.web;

import java.util.List;
import java.util.function.Function;

import pe.cevicherialourdes.shared.domain.model.Pagina;

/** Formato estable de todos los listados paginados de la API. */
public record PaginaResponse<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public static <D, T> PaginaResponse<T> de(Pagina<D> pagina, Function<D, T> mapeo) {
        return new PaginaResponse<>(pagina.contenido().stream().map(mapeo).toList(), pagina.pagina(),
                pagina.tamano(), pagina.totalElementos(), pagina.totalPaginas());
    }
}
