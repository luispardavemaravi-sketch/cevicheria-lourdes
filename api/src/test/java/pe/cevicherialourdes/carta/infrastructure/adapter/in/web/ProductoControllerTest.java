package pe.cevicherialourdes.carta.infrastructure.adapter.in.web;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pe.cevicherialourdes.support.ComoUsuario.admin;
import static pe.cevicherialourdes.support.ComoUsuario.cocina;
import static pe.cevicherialourdes.support.ComoUsuario.mozo;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.cevicherialourdes.carta.domain.model.Categoria;
import pe.cevicherialourdes.carta.domain.model.Presentacion;
import pe.cevicherialourdes.carta.domain.model.Producto;
import pe.cevicherialourdes.carta.domain.port.in.GestionarProductosUseCase;
import pe.cevicherialourdes.carta.domain.port.in.GestionarProductosUseCase.FiltroProductos;
import pe.cevicherialourdes.shared.domain.exception.ConflictoNegocioException;
import pe.cevicherialourdes.shared.domain.exception.RecursoNoEncontradoException;
import pe.cevicherialourdes.shared.domain.exception.ReglaNegocioException;
import pe.cevicherialourdes.shared.domain.model.Dinero;
import pe.cevicherialourdes.shared.domain.model.Pagina;
import pe.cevicherialourdes.shared.domain.model.Paginacion;
import pe.cevicherialourdes.support.ConfigWebPrueba;

@WebMvcTest(controllers = ProductoController.class)
@Import({ConfigWebPrueba.class, ProductoWebMapperImpl.class})
class ProductoControllerTest {

    private static final String URL = "/api/v1/productos";
    private static final String CEVICHE = """
            {"nombre":"Ceviche mixto","categoria":"PLATO","precio":18.00}
            """;

    @Autowired MockMvc mvc;
    @MockitoBean GestionarProductosUseCase productos;

    private static Producto ceviche() {
        return Producto.reconstituir(10L, "Ceviche mixto", Categoria.PLATO, Dinero.de("18"), true, true, true, List.of());
    }

    // ---- POST -------------------------------------------------------------------------------

    @Test
    void crear_comoAdmin_201ConLocation() throws Exception {
        when(productos.crear(any())).thenReturn(ceviche());

        mvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(CEVICHE))
           .andExpect(status().isCreated())
           .andExpect(header().string("Location", endsWith("/api/v1/productos/10")))
           .andExpect(jsonPath("$.id").value(10))
           .andExpect(jsonPath("$.precio").value(18.00))
           .andExpect(jsonPath("$.categoria").value("PLATO"))
           .andExpect(jsonPath("$.requiereCocina").value(true));
    }

    @Test
    void crear_sinToken_401() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CEVICHE))
           .andExpect(status().isUnauthorized());
        verifyNoInteractions(productos);
    }

    @Test
    void crear_comoMozo_403() throws Exception {
        mvc.perform(post(URL).with(mozo()).contentType(MediaType.APPLICATION_JSON).content(CEVICHE))
           .andExpect(status().isForbidden())
           .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
        verifyNoInteractions(productos);
    }

    @Test
    void crear_comoCocina_403() throws Exception {
        mvc.perform(post(URL).with(cocina()).contentType(MediaType.APPLICATION_JSON).content(CEVICHE))
           .andExpect(status().isForbidden());
    }

    @Test
    void crear_datosInvalidos_400() throws Exception {
        mvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"\",\"precio\":15.555}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.code").value("VALIDACION_FALLIDA"))
           .andExpect(jsonPath("$.fields[?(@.campo == 'nombre')]").exists())
           .andExpect(jsonPath("$.fields[?(@.campo == 'categoria')]").exists())
           .andExpect(jsonPath("$.fields[?(@.campo == 'precio')]").exists());
    }

    @Test
    void crear_categoriaInexistente_400() throws Exception {
        mvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"X\",\"categoria\":\"POSTRE\",\"precio\":5}"))
           .andExpect(status().isBadRequest());
    }

    @Test
    void crear_nombreRepetido_409() throws Exception {
        when(productos.crear(any()))
                .thenThrow(new ConflictoNegocioException("PRODUCTO_DUPLICADO", "Ya existe"));

        mvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(CEVICHE))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.code").value("PRODUCTO_DUPLICADO"));
    }

    @Test
    void crear_bebidaConPrecio_422() throws Exception {
        when(productos.crear(any()))
                .thenThrow(new ReglaNegocioException("PRECIO_EN_PRESENTACION", "Va en la presentacion"));

        mvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Chicha\",\"categoria\":\"BEBIDA\",\"precio\":3}"))
           .andExpect(status().isUnprocessableEntity())
           .andExpect(jsonPath("$.code").value("PRECIO_EN_PRESENTACION"));
    }

    @Test
    void crear_bebidaConPresentaciones_lasDevuelveConSuIdYPrecio_hu04() throws Exception {
        Producto chicha = Producto.reconstituir(11L, "Chicha", Categoria.BEBIDA, null, false, true, true,
                List.of(Presentacion.reconstituir(21L, "Vaso", Dinero.de("3.5"), true),
                        Presentacion.reconstituir(22L, "Jarra", Dinero.de("12"), false)));
        when(productos.crear(any())).thenReturn(chicha);

        mvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content("""
                {"nombre":"Chicha","categoria":"BEBIDA",
                 "presentaciones":[{"nombre":"Vaso","precio":3.50}]}
                """))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.precio").doesNotExist())
           .andExpect(jsonPath("$.presentaciones.length()").value(1))
           .andExpect(jsonPath("$.presentaciones[0].id").value(21))
           .andExpect(jsonPath("$.presentaciones[0].nombre").value("Vaso"))
           .andExpect(jsonPath("$.presentaciones[0].precio").value(3.50));
    }

    @Test
    void crear_presentacionInvalida_400ConElCampoAnidado() throws Exception {
        mvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content("""
                {"nombre":"Chicha","categoria":"BEBIDA",
                 "presentaciones":[{"nombre":"","precio":-1}]}
                """))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.fields[?(@.campo == 'presentaciones[0].nombre')]").exists())
           .andExpect(jsonPath("$.fields[?(@.campo == 'presentaciones[0].precio')]").exists());
        verifyNoInteractions(productos);
    }

    @Test
    void crear_bebidaSinPresentaciones_422() throws Exception {
        when(productos.crear(any()))
                .thenThrow(new ReglaNegocioException("PRESENTACION_REQUERIDA", "Falta presentacion"));

        mvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Chicha\",\"categoria\":\"BEBIDA\"}"))
           .andExpect(status().isUnprocessableEntity())
           .andExpect(jsonPath("$.code").value("PRESENTACION_REQUERIDA"));
    }

    // ---- PUT --------------------------------------------------------------------------------

    @Test
    void actualizar_comoAdmin_200() throws Exception {
        when(productos.actualizar(eq(10L), any())).thenReturn(ceviche());

        mvc.perform(put(URL + "/10").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ceviche mixto\",\"categoria\":\"PLATO\",\"precio\":18,\"activo\":true}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void actualizar_comoMozo_403() throws Exception {
        mvc.perform(put(URL + "/10").with(mozo()).contentType(MediaType.APPLICATION_JSON).content(CEVICHE))
           .andExpect(status().isForbidden());
    }

    @Test
    void actualizar_inexistente_404() throws Exception {
        when(productos.actualizar(eq(99L), any()))
                .thenThrow(new RecursoNoEncontradoException("PRODUCTO_NO_ENCONTRADO", "No existe"));

        mvc.perform(put(URL + "/99").with(admin()).contentType(MediaType.APPLICATION_JSON).content(CEVICHE))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.code").value("PRODUCTO_NO_ENCONTRADO"));
    }

    @Test
    void actualizar_idNoNumerico_400() throws Exception {
        mvc.perform(put(URL + "/abc").with(admin()).contentType(MediaType.APPLICATION_JSON).content(CEVICHE))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.code").value("PARAMETRO_INVALIDO"));
    }

    // ---- GET --------------------------------------------------------------------------------

    @Test
    void listar_cualquierRolAutenticado_paginado() throws Exception {
        when(productos.listar(new FiltroProductos(Categoria.PLATO, false), new Paginacion(1, 5)))
                .thenReturn(new Pagina<>(List.of(ceviche()), 1, 5, 6));

        mvc.perform(get(URL).with(cocina()).param("categoria", "PLATO").param("page", "1").param("size", "5"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.contenido[0].nombre").value("Ceviche mixto"))
           .andExpect(jsonPath("$.pagina").value(1))
           .andExpect(jsonPath("$.tamano").value(5))
           .andExpect(jsonPath("$.totalElementos").value(6))
           .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    void listar_tamanoExcesivo_seLimitaA100() throws Exception {
        when(productos.listar(any(), any())).thenReturn(new Pagina<>(List.of(), 0, 100, 0));

        mvc.perform(get(URL).with(mozo()).param("size", "5000"))
           .andExpect(status().isOk());
        verify(productos).listar(new FiltroProductos(null, false), new Paginacion(0, 100));
    }

    @Test
    void listar_sinToken_401() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }
}
