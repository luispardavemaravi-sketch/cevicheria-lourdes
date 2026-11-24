package pe.cevicherialourdes.carta.domain.port.in;

import java.math.BigDecimal;
import java.util.List;

import pe.cevicherialourdes.carta.domain.model.Categoria;
import pe.cevicherialourdes.carta.domain.model.Producto;
import pe.cevicherialourdes.shared.domain.model.Pagina;
import pe.cevicherialourdes.shared.domain.model.Paginacion;

/**
 * HU-03: CRUD de productos (la baja es logica, via actualizar con activo = false).
 * HU-04: las presentaciones de las bebidas viajan dentro del producto.
 * HU-05: los acompanamientos de los platos tambien (por nombre).
 */
public interface GestionarProductosUseCase {

    /**
     * @param requiereCocina null = valor por defecto de la categoria
     * @param activo         null = true
     * @param presentaciones  lista completa deseada (solo BEBIDA)
     * @param acompanamientos nombres; lista completa deseada (solo PLATO); null = ninguno
     */
    record DatosProducto(String nombre, Categoria categoria, BigDecimal precio, Boolean requiereCocina,
                         Boolean activo, List<DatosPresentacion> presentaciones, List<String> acompanamientos) {
    }

    record DatosPresentacion(String nombre, BigDecimal precio) {
    }

    /** @param categoria null = todas */
    record FiltroProductos(Categoria categoria, boolean incluirInactivos) {
    }

    Producto crear(DatosProducto datos);

    Producto actualizar(Long id, DatosProducto datos);

    Pagina<Producto> listar(FiltroProductos filtro, Paginacion paginacion);
}
