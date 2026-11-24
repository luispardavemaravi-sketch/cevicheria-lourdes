package pe.cevicherialourdes.carta.domain.port.out;

import java.util.Optional;

import pe.cevicherialourdes.carta.domain.model.Producto;
import pe.cevicherialourdes.carta.domain.port.in.GestionarProductosUseCase.FiltroProductos;
import pe.cevicherialourdes.shared.domain.model.Pagina;
import pe.cevicherialourdes.shared.domain.model.Paginacion;

public interface ProductoRepositorio {

    Optional<Producto> buscarPorId(Long id);

    /** Comparacion sin importar mayusculas. @param excluirId id propio al editar (null al crear). */
    boolean existeNombre(String nombre, Long excluirId);

    Producto guardar(Producto producto);

    /** Ordenado por nombre. */
    Pagina<Producto> listar(FiltroProductos filtro, Paginacion paginacion);
}
