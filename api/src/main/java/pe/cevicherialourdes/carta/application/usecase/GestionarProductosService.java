package pe.cevicherialourdes.carta.application.usecase;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.cevicherialourdes.carta.domain.model.DefinicionPresentacion;
import pe.cevicherialourdes.carta.domain.model.Producto;
import pe.cevicherialourdes.carta.domain.port.in.GestionarProductosUseCase;
import pe.cevicherialourdes.carta.domain.port.out.ProductoRepositorio;
import pe.cevicherialourdes.shared.domain.exception.ConflictoNegocioException;
import pe.cevicherialourdes.shared.domain.exception.RecursoNoEncontradoException;
import pe.cevicherialourdes.shared.domain.model.Dinero;
import pe.cevicherialourdes.shared.domain.model.Pagina;
import pe.cevicherialourdes.shared.domain.model.Paginacion;

@Service
public class GestionarProductosService implements GestionarProductosUseCase {

    private static final Logger log = LoggerFactory.getLogger(GestionarProductosService.class);

    private final ProductoRepositorio repositorio;

    public GestionarProductosService(ProductoRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional
    public Producto crear(DatosProducto datos) {
        Producto producto = Producto.crear(datos.nombre(), datos.categoria(), dinero(datos), datos.requiereCocina(),
                presentaciones(datos));
        exigirNombreLibre(producto.getNombre(), null);
        Producto guardado = repositorio.guardar(producto);
        log.info("Producto {} creado ({})", guardado.getId(), guardado.getCategoria());
        return guardado;
    }

    @Override
    @Transactional
    public Producto actualizar(Long id, DatosProducto datos) {
        Producto producto = repositorio.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("PRODUCTO_NO_ENCONTRADO",
                        "El producto " + id + " no existe"));
        exigirNombreLibre(Producto.normalizarNombre(datos.nombre()), id);
        producto.actualizar(datos.nombre(), datos.categoria(), dinero(datos), datos.requiereCocina(),
                datos.activo() == null || datos.activo(), presentaciones(datos));
        Producto guardado = repositorio.guardar(producto);
        log.info("Producto {} actualizado (activo={})", id, guardado.isActivo());
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Producto> listar(FiltroProductos filtro, Paginacion paginacion) {
        return repositorio.listar(filtro, paginacion);
    }

    private void exigirNombreLibre(String nombre, Long excluirId) {
        if (repositorio.existeNombre(nombre, excluirId)) {
            throw new ConflictoNegocioException("PRODUCTO_DUPLICADO", "Ya existe un producto llamado '" + nombre + "'");
        }
    }

    private static Dinero dinero(DatosProducto datos) {
        return datos.precio() == null ? null : Dinero.de(datos.precio());
    }

    private static List<DefinicionPresentacion> presentaciones(DatosProducto datos) {
        if (datos.presentaciones() == null) {
            return null;
        }
        return datos.presentaciones().stream()
                .map(p -> new DefinicionPresentacion(p.nombre(), p.precio() == null ? null : Dinero.de(p.precio())))
                .toList();
    }
}
