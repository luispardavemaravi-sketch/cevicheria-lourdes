package pe.cevicherialourdes.carta.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import pe.cevicherialourdes.shared.domain.exception.ReglaNegocioException;
import pe.cevicherialourdes.shared.domain.model.Dinero;

/** HU-05: acompanamientos por plato (sin precio). */
class ProductoAcompanamientosTest {

    private static final Acompanamiento CAMOTE = new Acompanamiento(1L, "Camote");
    private static final Acompanamiento CEBOLLA = new Acompanamiento(2L, "Cebolla");

    private static Producto ceviche() {
        return Producto.crear("Ceviche", Categoria.PLATO, Dinero.de("15"), null, null);
    }

    @Test
    void plato_recibeSusAcompanamientosOrdenadosPorNombre() {
        Producto p = ceviche();
        p.asignarAcompanamientos(List.of(CEBOLLA, CAMOTE));

        assertThat(p.getAcompanamientos()).containsExactly(CAMOTE, CEBOLLA);
    }

    @Test
    void asignar_reemplazaLaListaAnterior() {
        Producto p = ceviche();
        p.asignarAcompanamientos(List.of(CAMOTE, CEBOLLA));
        p.asignarAcompanamientos(List.of(CEBOLLA));

        assertThat(p.getAcompanamientos()).containsExactly(CEBOLLA);
    }

    @Test
    void platoSinAcompanamientos_esValido() {
        Producto p = ceviche();
        p.asignarAcompanamientos(List.of());
        assertThat(p.getAcompanamientos()).isEmpty();
    }

    @Test
    void bebidaOTaperConAcompanamientos_422() {
        Producto taper = Producto.crear("Taper", Categoria.TAPER, Dinero.de("1"), null, null);

        assertThatThrownBy(() -> taper.asignarAcompanamientos(List.of(CAMOTE)))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("ACOMPANAMIENTO_SOLO_EN_PLATO");
        taper.asignarAcompanamientos(List.of());   // vacio si se permite
    }

    @Test
    void elMismoAcompanamientoDosVeces_422() {
        assertThatThrownBy(() -> ceviche().asignarAcompanamientos(List.of(CAMOTE, new Acompanamiento(1L, "camote"))))
                .extracting("codigo").isEqualTo("ACOMPANAMIENTO_DUPLICADO");
    }

    // ---- Nombres que manda el ADMIN -----------------------------------------------------------

    @Test
    void normalizarNombres_recortaYColapsaEspacios() {
        assertThat(Acompanamiento.normalizarNombres(List.of("  Porcion  de ceviche ", "Camote")))
                .containsExactly("Porcion de ceviche", "Camote");
    }

    @Test
    void normalizarNombres_nullEsListaVacia() {
        assertThat(Acompanamiento.normalizarNombres(null)).isEmpty();
    }

    @Test
    void normalizarNombres_rechazaRepetidosSinImportarMayusculas() {
        assertThatThrownBy(() -> Acompanamiento.normalizarNombres(List.of("Camote", " CAMOTE")))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo("ACOMPANAMIENTO_DUPLICADO");
    }

    @Test
    void normalizarNombres_rechazaVaciosYDemasiadoLargos() {
        assertThatThrownBy(() -> Acompanamiento.normalizarNombres(List.of("  ")))
                .extracting("codigo").isEqualTo("ACOMPANAMIENTO_INVALIDO");
        assertThatThrownBy(() -> Acompanamiento.normalizarNombres(Arrays.asList("Camote", null)))
                .extracting("codigo").isEqualTo("ACOMPANAMIENTO_INVALIDO");
        assertThatThrownBy(() -> Acompanamiento.normalizarNombres(List.of("x".repeat(41))))
                .extracting("codigo").isEqualTo("ACOMPANAMIENTO_INVALIDO");
    }
}
