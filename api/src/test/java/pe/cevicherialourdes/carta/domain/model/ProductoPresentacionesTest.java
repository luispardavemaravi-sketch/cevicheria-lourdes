package pe.cevicherialourdes.carta.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import pe.cevicherialourdes.shared.domain.exception.ReglaNegocioException;
import pe.cevicherialourdes.shared.domain.model.Dinero;

/** HU-04: presentaciones de bebidas con precio propio; pedir una bebida exige presentacion. */
class ProductoPresentacionesTest {

    private static DefinicionPresentacion def(String nombre, String precio) {
        return new DefinicionPresentacion(nombre, Dinero.de(precio));
    }

    private static Producto incaKola() {
        List<Presentacion> existentes = new ArrayList<>(List.of(
                Presentacion.reconstituir(1L, "Personal", Dinero.de("2.00"), true),
                Presentacion.reconstituir(2L, "1 L", Dinero.de("6.00"), true),
                Presentacion.reconstituir(3L, "Gordita", Dinero.de("4.00"), false)));
        return Producto.reconstituir(7L, "Inca Kola", Categoria.BEBIDA, null, false, true, true, existentes);
    }

    @Test
    void bebida_seCreaConSusPresentacionesOrdenadasPorPrecio() {
        Producto p = Producto.crear("Chicha morada", Categoria.BEBIDA, null, null,
                List.of(def("Jarra", "12"), def("Vaso", "3.5")));

        assertThat(p.presentacionesActivas())
                .extracting(Presentacion::getNombre, Presentacion::getPrecio)
                .containsExactly(tuple("Vaso", Dinero.de("3.50")), tuple("Jarra", Dinero.de("12.00")));
    }

    @Test
    void bebida_sinPresentaciones_422() {
        assertThatThrownBy(() -> Producto.crear("Chicha", Categoria.BEBIDA, null, null, List.of()))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("PRESENTACION_REQUERIDA");
        assertThatThrownBy(() -> Producto.crear("Chicha", Categoria.BEBIDA, null, null, null))
                .extracting("codigo").isEqualTo("PRESENTACION_REQUERIDA");
    }

    @Test
    void platoConPresentaciones_422() {
        assertThatThrownBy(() -> Producto.crear("Ceviche", Categoria.PLATO, Dinero.de("15"), null,
                List.of(def("Fuente", "30"))))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("PRESENTACION_SOLO_EN_BEBIDA");
    }

    @Test
    void presentacionesRepetidasSinImportarMayusculas_422() {
        assertThatThrownBy(() -> Producto.crear("Chicha", Categoria.BEBIDA, null, null,
                List.of(def("Vaso", "3"), def(" vaso ", "4"))))
                .extracting("codigo").isEqualTo("PRESENTACION_DUPLICADA");
    }

    @Test
    void presentacion_validaNombreYPrecio() {
        assertThatThrownBy(() -> Presentacion.nueva(" ", Dinero.de("1")))
                .extracting("codigo").isEqualTo("PRESENTACION_INVALIDA");
        assertThatThrownBy(() -> Presentacion.nueva("x".repeat(31), Dinero.de("1")))
                .extracting("codigo").isEqualTo("PRESENTACION_INVALIDA");
        assertThatThrownBy(() -> Presentacion.nueva("Vaso", Dinero.de("0")))
                .extracting("codigo").isEqualTo("PRECIO_INVALIDO");
        assertThatThrownBy(() -> Presentacion.nueva("Vaso", null))
                .extracting("codigo").isEqualTo("PRECIO_INVALIDO");
    }

    @Test
    void redefinir_actualizaPrecios_reactiva_agrega_yDesactivaLasQueYaNoEstan() {
        Producto p = incaKola();

        p.actualizar("Inca Kola", Categoria.BEBIDA, null, null, true, List.of(
                def("personal", "2.50"),   // existente: cambia precio (y mayusculas)
                def("Gordita", "4.00"),    // estaba inactiva: se reactiva, conserva id
                def("3 L", "15.00")));     // nueva

        assertThat(p.presentacionesActivas())
                .extracting(Presentacion::getId, Presentacion::getNombre, Presentacion::getPrecio)
                .containsExactly(
                        tuple(1L, "personal", Dinero.de("2.50")),
                        tuple(3L, "Gordita", Dinero.de("4.00")),
                        tuple(null, "3 L", Dinero.de("15.00")));
        // "1 L" no se borra (la referencian pedidos viejos): queda inactiva
        assertThat(p.getPresentaciones())
                .filteredOn(pr -> pr.getId() != null && pr.getId() == 2L)
                .singleElement()
                .satisfies(pr -> assertThat(pr.isActivo()).isFalse());
    }

    @Test
    void redefinirConListaInvalida_noModificaNada() {
        Producto p = incaKola();

        assertThatThrownBy(() -> p.actualizar("Inca Kola", Categoria.BEBIDA, null, null, true,
                List.of(def("Personal", "3"), def("PERSONAL", "4"))))
                .extracting("codigo").isEqualTo("PRESENTACION_DUPLICADA");
        assertThat(p.presentacionesActivas()).extracting(Presentacion::getPrecio)
                .containsExactly(Dinero.de("2.00"), Dinero.de("6.00"));
    }

    // ---- Precio al pedir (lo usara HU-07) ---------------------------------------------------

    @Test
    void pedirBebidaSinPresentacion_422() {
        assertThatThrownBy(() -> incaKola().precioPara(null))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("PRESENTACION_REQUERIDA");
    }

    @Test
    void pedirBebidaConPresentacion_usaElPrecioDeLaPresentacion() {
        assertThat(incaKola().precioPara(2L)).isEqualTo(Dinero.de("6.00"));
    }

    @Test
    void pedirConPresentacionInactivaODeOtroProducto_422() {
        assertThatThrownBy(() -> incaKola().precioPara(3L))
                .extracting("codigo").isEqualTo("PRESENTACION_NO_VALIDA");
        assertThatThrownBy(() -> incaKola().precioPara(999L))
                .extracting("codigo").isEqualTo("PRESENTACION_NO_VALIDA");
    }

    @Test
    void pedirPlatoConPresentacion_422_ySinElla_usaElPrecioDelPlato() {
        Producto ceviche = Producto.crear("Ceviche", Categoria.PLATO, Dinero.de("15"), null, null);
        assertThatThrownBy(() -> ceviche.precioPara(1L))
                .extracting("codigo").isEqualTo("PRESENTACION_NO_APLICA");
        assertThat(ceviche.precioPara(null)).isEqualTo(Dinero.de("15.00"));
    }
}
