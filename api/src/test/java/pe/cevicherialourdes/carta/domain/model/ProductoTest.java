package pe.cevicherialourdes.carta.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import pe.cevicherialourdes.shared.domain.exception.ReglaNegocioException;
import pe.cevicherialourdes.shared.domain.model.Dinero;

class ProductoTest {

    @Test
    void plato_seCreaConPrecioYVaACocinaPorDefecto() {
        Producto p = Producto.crear("  Ceviche   de pescado ", Categoria.PLATO, Dinero.de("15"), null);
        assertThat(p.getNombre()).isEqualTo("Ceviche de pescado");
        assertThat(p.getPrecio()).isEqualTo(Dinero.de("15.00"));
        assertThat(p.isRequiereCocina()).isTrue();
        assertThat(p.isDisponible()).isTrue();
        assertThat(p.isActivo()).isTrue();
    }

    @Test
    void rn06_bebidaYTaperNoVanACocinaPorDefecto() {
        assertThat(Producto.crear("Taper", Categoria.TAPER, Dinero.de("1"), null).isRequiereCocina()).isFalse();
        assertThat(Producto.crear("Chicha", Categoria.BEBIDA, null, null).isRequiereCocina()).isFalse();
    }

    @Test
    void requiereCocinaExplicitoGanaAlDefecto() {
        assertThat(Producto.crear("Postre", Categoria.PLATO, Dinero.de("5"), false).isRequiereCocina()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = Categoria.class, names = {"PLATO", "TAPER"})
    void platoYTaper_exigenPrecio(Categoria categoria) {
        assertThatThrownBy(() -> Producto.crear("X", categoria, null, null))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("PRECIO_INVALIDO");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.00", "1000000.00"})
    void precioDebeSerMayorQueCeroYCaberEnLaBase(String precio) {
        assertThatThrownBy(() -> Producto.crear("X", Categoria.PLATO, Dinero.de(precio), null))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("PRECIO_INVALIDO");
    }

    @Test
    void bebida_noLlevaPrecioPropio_vaEnSusPresentaciones() {
        assertThatThrownBy(() -> Producto.crear("Inca Kola", Categoria.BEBIDA, Dinero.de("2"), null))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("PRECIO_EN_PRESENTACION");
    }

    @Test
    void nombreObligatorioYMaximo60() {
        assertThatThrownBy(() -> Producto.crear("   ", Categoria.PLATO, Dinero.de("1"), null))
                .extracting("codigo").isEqualTo("NOMBRE_INVALIDO");
        assertThatThrownBy(() -> Producto.crear("x".repeat(61), Categoria.PLATO, Dinero.de("1"), null))
                .extracting("codigo").isEqualTo("NOMBRE_INVALIDO");
        assertThat(Producto.crear("x".repeat(60), Categoria.PLATO, Dinero.de("1"), null).getNombre()).hasSize(60);
    }

    @Test
    void actualizar_cambiaDatosYPermiteDesactivar() {
        Producto p = Producto.reconstituir(3L, "Chilcano", Categoria.PLATO, Dinero.de("6"), true, true, true);

        p.actualizar("Chilcano especial", Categoria.PLATO, Dinero.de("8"), true, false);

        assertThat(p.getNombre()).isEqualTo("Chilcano especial");
        assertThat(p.getPrecio()).isEqualTo(Dinero.de("8"));
        assertThat(p.isActivo()).isFalse();
    }

    @Test
    void actualizar_noPermiteCambiarLaCategoria() {
        Producto p = Producto.reconstituir(3L, "Chilcano", Categoria.PLATO, Dinero.de("6"), true, true, true);

        assertThatThrownBy(() -> p.actualizar("Chilcano", Categoria.BEBIDA, null, null, true))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("CATEGORIA_NO_MODIFICABLE");
    }

    @Test
    void actualizar_validaIgualQueAlCrear() {
        Producto p = Producto.reconstituir(3L, "Chilcano", Categoria.PLATO, Dinero.de("6"), true, true, true);

        assertThatThrownBy(() -> p.actualizar("Chilcano", Categoria.PLATO, Dinero.de("0"), null, true))
                .extracting("codigo").isEqualTo("PRECIO_INVALIDO");
        assertThat(p.getPrecio()).isEqualTo(Dinero.de("6"));
    }
}
