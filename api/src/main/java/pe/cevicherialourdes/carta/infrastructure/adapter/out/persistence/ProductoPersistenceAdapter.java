package pe.cevicherialourdes.carta.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import pe.cevicherialourdes.carta.domain.model.Producto;
import pe.cevicherialourdes.carta.domain.port.in.GestionarProductosUseCase.FiltroProductos;
import pe.cevicherialourdes.carta.domain.port.out.ProductoRepositorio;
import pe.cevicherialourdes.shared.domain.model.Pagina;
import pe.cevicherialourdes.shared.domain.model.Paginacion;

@Component
class ProductoPersistenceAdapter implements ProductoRepositorio {

    private static final Sort POR_NOMBRE = Sort.by("nombre").and(Sort.by("id"));

    private final ProductoJpaRepository jpa;

    ProductoPersistenceAdapter(ProductoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Producto> buscarPorId(Long id) {
        return jpa.findById(id).map(ProductoEntity::aDominio);
    }

    @Override
    public boolean existeNombre(String nombre, Long excluirId) {
        return jpa.existeNombre(nombre, excluirId);
    }

    @Override
    public Producto guardar(Producto producto) {
        return jpa.save(ProductoEntity.desde(producto)).aDominio();
    }

    @Override
    public Pagina<Producto> listar(FiltroProductos filtro, Paginacion paginacion) {
        Page<ProductoEntity> pagina = jpa.buscar(filtro.categoria(), filtro.incluirInactivos(),
                PageRequest.of(paginacion.pagina(), paginacion.tamano(), POR_NOMBRE));
        return new Pagina<>(pagina.map(ProductoEntity::aDominio).getContent(), paginacion.pagina(),
                paginacion.tamano(), pagina.getTotalElements());
    }
}
