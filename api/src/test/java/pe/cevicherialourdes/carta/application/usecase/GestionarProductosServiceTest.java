package pe.cevicherialourdes.carta.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.cevicherialourdes.carta.domain.model.Categoria;
import pe.cevicherialourdes.carta.domain.model.Producto;
import pe.cevicherialourdes.carta.domain.port.in.GestionarProductosUseCase.DatosPresentacion;
import pe.cevicherialourdes.carta.domain.port.in.GestionarProductosUseCase.DatosProducto;
import pe.cevicherialourdes.carta.domain.port.in.GestionarProductosUseCase.FiltroProductos;
import pe.cevicherialourdes.carta.domain.port.out.ProductoRepositorio;
import pe.cevicherialourdes.shared.domain.exception.ConflictoNegocioException;
import pe.cevicherialourdes.shared.domain.exception.RecursoNoEncontradoException;
import pe.cevicherialourdes.shared.domain.model.Dinero;
import pe.cevicherialourdes.shared.domain.model.Pagina;
import pe.cevicherialourdes.shared.domain.model.Paginacion;

@ExtendWith(MockitoExtension.class)
class GestionarProductosServiceTest {

    @Mock ProductoRepositorio repositorio;
    @InjectMocks GestionarProductosService servicio;

    private static DatosProducto ceviche(String precio) {
        return new DatosProducto("Ceviche mixto", Categoria.PLATO, new BigDecimal(precio), null, null, null);
    }

    @Test
    void crear_guardaElProductoValidado() {
        when(repositorio.existeNombre("Ceviche mixto", null)).thenReturn(false);
        when(repositorio.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Producto creado = servicio.crear(ceviche("18.00"));

        assertThat(creado.getNombre()).isEqualTo("Ceviche mixto");
        assertThat(creado.getPrecio()).isEqualTo(Dinero.de("18"));
        assertThat(creado.isActivo()).isTrue();
    }

    @Test
    void crear_nombreRepetido_409() {
        when(repositorio.existeNombre("Ceviche mixto", null)).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(ceviche("18.00")))
                .isInstanceOf(ConflictoNegocioException.class)
                .extracting("codigo").isEqualTo("PRODUCTO_DUPLICADO");
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void actualizar_productoInexistente_404() {
        when(repositorio.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizar(99L, ceviche("18.00")))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .extracting("codigo").isEqualTo("PRODUCTO_NO_ENCONTRADO");
    }

    @Test
    void actualizar_noChocaConsigoMismoPeroSiConOtro() {
        Producto existente = Producto.reconstituir(5L, "Ceviche", Categoria.PLATO, Dinero.de("15"), true, true, true, List.of());
        when(repositorio.buscarPorId(5L)).thenReturn(Optional.of(existente));
        when(repositorio.existeNombre("Ceviche mixto", 5L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.actualizar(5L, ceviche("18.00")))
                .isInstanceOf(ConflictoNegocioException.class);
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void actualizar_aplicaCambiosYGuarda() {
        Producto existente = Producto.reconstituir(5L, "Ceviche", Categoria.PLATO, Dinero.de("15"), true, true, true, List.of());
        when(repositorio.buscarPorId(5L)).thenReturn(Optional.of(existente));
        when(repositorio.existeNombre("Ceviche mixto", 5L)).thenReturn(false);
        when(repositorio.guardar(existente)).thenReturn(existente);

        Producto actualizado = servicio.actualizar(5L,
                new DatosProducto("Ceviche mixto", Categoria.PLATO, new BigDecimal("18.00"), true, false, null));

        assertThat(actualizado.getPrecio()).isEqualTo(Dinero.de("18"));
        assertThat(actualizado.isActivo()).isFalse();
    }

    @Test
    void crear_bebidaConSusPresentaciones_hu04() {
        when(repositorio.existeNombre("Chicha morada", null)).thenReturn(false);
        when(repositorio.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Producto creado = servicio.crear(new DatosProducto("Chicha morada", Categoria.BEBIDA, null, null, null,
                List.of(new DatosPresentacion("Vaso", new BigDecimal("3.50")),
                        new DatosPresentacion("Jarra", new BigDecimal("12")))));

        assertThat(creado.presentacionesActivas()).extracting(p -> p.getPrecio())
                .containsExactly(Dinero.de("3.50"), Dinero.de("12.00"));
    }

    @Test
    void listar_delegaConElFiltroYLaPaginacion() {
        FiltroProductos filtro = new FiltroProductos(Categoria.BEBIDA, false);
        Paginacion paginacion = new Paginacion(0, 20);
        Pagina<Producto> pagina = new Pagina<>(List.of(), 0, 20, 0);
        when(repositorio.listar(filtro, paginacion)).thenReturn(pagina);

        assertThat(servicio.listar(filtro, paginacion)).isSameAs(pagina);
    }
}
