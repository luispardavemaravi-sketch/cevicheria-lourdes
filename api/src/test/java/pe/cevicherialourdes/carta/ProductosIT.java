package pe.cevicherialourdes.carta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import pe.cevicherialourdes.support.IntegracionBase;

/** HU-03 contra PostgreSQL real: alta, nombre unico sin importar mayusculas, edicion, baja logica, listado. */
@SuppressWarnings({"rawtypes", "unchecked"})
class ProductosIT extends IntegracionBase {

    private static final String URL = "/api/v1/productos";

    private ResponseEntity<Map> crear(String token, Map<String, Object> cuerpo) {
        return llamar(HttpMethod.POST, URL, token, cuerpo, Map.class);
    }

    private List<Map<String, Object>> listar(String token, String query) {
        ResponseEntity<Map> r = llamar(HttpMethod.GET, URL + query, token, null, Map.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (List<Map<String, Object>>) r.getBody().get("contenido");
    }

    @Test
    void adminCreaUnPlato_yLoGuardaConPrecioExacto() {
        String nombre = unico("Leche de tigre");
        ResponseEntity<Map> r = crear(tokenAdmin(), Map.of("nombre", nombre, "categoria", "PLATO", "precio", 8.5));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r.getHeaders().getLocation()).isNotNull();
        Number id = (Number) r.getBody().get("id");
        assertThat(jdbc.queryForObject("SELECT precio FROM producto WHERE id = ?", BigDecimal.class, id.longValue()))
                .isEqualByComparingTo("8.50");
    }

    @Test
    void nombreUnico_sinImportarMayusculasNiEspacios_409() {
        String nombre = unico("Sudado");
        assertThat(crear(tokenAdmin(), Map.of("nombre", nombre, "categoria", "PLATO", "precio", 20))
                .getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<Map> repetido = crear(tokenAdmin(),
                Map.of("nombre", "  " + nombre.toUpperCase() + " ", "categoria", "PLATO", "precio", 21));
        assertThat(repetido.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(repetido.getBody()).containsEntry("code", "PRODUCTO_DUPLICADO");
    }

    @Test
    void laBaseTambienImpideNombresRepetidosConDistintasMayusculas_V3() {
        String nombre = unico("Jalea");
        jdbc.update("INSERT INTO producto (nombre, categoria, precio) VALUES (?, 'PLATO', 25)", nombre);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO producto (nombre, categoria, precio) VALUES (?, 'PLATO', 25)", nombre.toLowerCase()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void mozoNoPuedeCrear_403() {
        ResponseEntity<Map> r = crear(tokenDe("MOZO"), Map.of("nombre", unico("X"), "categoria", "PLATO", "precio", 1));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void bebidaConPrecioPropio_422() {
        ResponseEntity<Map> r = crear(tokenAdmin(), Map.of("nombre", unico("Chicha"), "categoria", "BEBIDA", "precio", 3));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r.getBody()).containsEntry("code", "PRECIO_EN_PRESENTACION");
    }

    @Test
    void editarYDesactivar_ocultaElProductoDelListadoNormal() {
        String token = tokenAdmin();
        String nombre = unico("Tiradito");
        Number id = (Number) crear(token, Map.of("nombre", nombre, "categoria", "PLATO", "precio", 16))
                .getBody().get("id");

        ResponseEntity<Map> editado = llamar(HttpMethod.PUT, URL + "/" + id, token,
                Map.of("nombre", nombre, "categoria", "PLATO", "precio", 17.5, "activo", false), Map.class);
        assertThat(editado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(editado.getBody()).containsEntry("activo", false);

        String mozo = tokenDe("MOZO");
        assertThat(listar(mozo, "?size=100")).extracting(p -> p.get("nombre")).doesNotContain(nombre);
        assertThat(listar(token, "?size=100&incluirInactivos=true")).extracting(p -> p.get("nombre")).contains(nombre);
    }

    @Test
    void cambiarCategoria_422() {
        String token = tokenAdmin();
        String nombre = unico("Causa");
        Number id = (Number) crear(token, Map.of("nombre", nombre, "categoria", "PLATO", "precio", 9))
                .getBody().get("id");

        ResponseEntity<Map> r = llamar(HttpMethod.PUT, URL + "/" + id, token,
                Map.of("nombre", nombre, "categoria", "TAPER", "precio", 9, "activo", true), Map.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r.getBody()).containsEntry("code", "CATEGORIA_NO_MODIFICABLE");
    }

    @Test
    void listado_filtraPorCategoriaYOrdenaPorNombre() {
        List<Map<String, Object>> bebidas = listar(tokenDe("COCINA"), "?categoria=BEBIDA&size=100");

        assertThat(bebidas).isNotEmpty().allSatisfy(b -> assertThat(b.get("categoria")).isEqualTo("BEBIDA"));
        assertThat(bebidas).extracting(b -> (String) b.get("nombre")).isSorted();
    }

    // ---- HU-04: presentaciones --------------------------------------------------------------

    @Test
    void laCartaInicialMuestraLasPresentacionesDeIncaKolaOrdenadasPorPrecio() {
        Map<String, Object> incaKola = listar(tokenDe("MOZO"), "?categoria=BEBIDA&size=100").stream()
                .filter(b -> "Inca Kola".equals(b.get("nombre"))).findFirst().orElseThrow();

        List<Map<String, Object>> presentaciones = (List<Map<String, Object>>) incaKola.get("presentaciones");
        assertThat(presentaciones).extracting(p -> p.get("nombre"))
                .containsExactly("Personal", "Gordita", "1 L", "1.5 L");
        assertThat(presentaciones).allSatisfy(p -> assertThat(p.get("id")).isNotNull());
    }

    @Test
    void bebida_seCreaConPresentaciones_yAlEditarLasQueSalenQuedanInactivas() {
        String token = tokenAdmin();
        String nombre = unico("Chicha");
        ResponseEntity<Map> creada = crear(token, Map.of("nombre", nombre, "categoria", "BEBIDA",
                "presentaciones", List.of(Map.of("nombre", "Vaso", "precio", 3.5),
                                          Map.of("nombre", "Jarra", "precio", 12))));
        assertThat(creada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Number id = (Number) creada.getBody().get("id");
        Number idVaso = (Number) ((List<Map<String, Object>>) creada.getBody().get("presentaciones")).get(0).get("id");

        ResponseEntity<Map> editada = llamar(HttpMethod.PUT, URL + "/" + id, token, Map.of(
                "nombre", nombre, "categoria", "BEBIDA", "activo", true,
                "presentaciones", List.of(Map.of("nombre", "vaso", "precio", 4))), Map.class);

        assertThat(editada.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> activas = (List<Map<String, Object>>) editada.getBody().get("presentaciones");
        assertThat(activas).singleElement().satisfies(p -> {
            assertThat(p.get("id")).isEqualTo(idVaso);        // misma fila: los pedidos viejos siguen apuntando bien
            assertThat(((Number) p.get("precio")).doubleValue()).isEqualTo(4.0);
        });
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM presentacion WHERE producto_id = ? AND activo = FALSE", Integer.class,
                id.longValue())).isEqualTo(1);
    }

    @Test
    void bebidaSinPresentaciones_422_yPlatoConPresentaciones_422() {
        String token = tokenAdmin();
        assertThat(crear(token, Map.of("nombre", unico("Emoliente"), "categoria", "BEBIDA")).getBody())
                .containsEntry("code", "PRESENTACION_REQUERIDA");
        assertThat(crear(token, Map.of("nombre", unico("Ronda"), "categoria", "PLATO", "precio", 40,
                "presentaciones", List.of(Map.of("nombre", "Grande", "precio", 50)))).getBody())
                .containsEntry("code", "PRESENTACION_SOLO_EN_BEBIDA");
    }

    // ---- HU-05: acompanamientos ---------------------------------------------------------------

    @Test
    void laCartaInicialMuestraLosAcompanamientosDelCeviche() {
        Map<String, Object> ceviche = listar(tokenDe("MOZO"), "?categoria=PLATO&size=100").stream()
                .filter(p -> "Ceviche de pescado".equals(p.get("nombre"))).findFirst().orElseThrow();

        assertThat((List<Map<String, Object>>) ceviche.get("acompanamientos")).extracting(a -> a.get("nombre"))
                .containsExactly("Camote", "Cebolla", "Chicharrón", "Lechuga");
    }

    @Test
    void plato_reusaAcompanamientosExistentesSinImportarMayusculas_yCreaLosNuevos() {
        String token = tokenAdmin();
        String nuevo = unico("Yuca");
        Integer catalogoAntes = jdbc.queryForObject("SELECT count(*) FROM acompanamiento", Integer.class);

        ResponseEntity<Map> r = crear(token, Map.of("nombre", unico("Pescado frito"), "categoria", "PLATO",
                "precio", 18, "acompanamientos", List.of("camote", nuevo)));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat((List<Map<String, Object>>) r.getBody().get("acompanamientos")).extracting(a -> a.get("nombre"))
                .containsExactly("Camote", nuevo);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM acompanamiento", Integer.class))
                .isEqualTo(catalogoAntes + 1);
    }

    @Test
    void editar_reemplazaLosAcompanamientosDelPlato() {
        String token = tokenAdmin();
        String nombre = unico("Chicharron de pota");
        Number id = (Number) crear(token, Map.of("nombre", nombre, "categoria", "PLATO", "precio", 12,
                "acompanamientos", List.of("Camote", "Cebolla", "Cancha"))).getBody().get("id");

        ResponseEntity<Map> r = llamar(HttpMethod.PUT, URL + "/" + id, token, Map.of("nombre", nombre,
                "categoria", "PLATO", "precio", 12, "activo", true, "acompanamientos", List.of("Cancha")), Map.class);

        assertThat((List<Map<String, Object>>) r.getBody().get("acompanamientos")).extracting(a -> a.get("nombre"))
                .containsExactly("Cancha");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM producto_acompanamiento WHERE producto_id = ?",
                Integer.class, id.longValue())).isEqualTo(1);
    }

    @Test
    void taperConAcompanamientos_422_yNoDejaBasuraEnElCatalogo() {
        String nuevo = unico("Ají");
        ResponseEntity<Map> r = crear(tokenAdmin(), Map.of("nombre", unico("Taper grande"), "categoria", "TAPER",
                "precio", 2, "acompanamientos", List.of(nuevo)));

        assertThat(r.getBody()).containsEntry("code", "ACOMPANAMIENTO_SOLO_EN_PLATO");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM acompanamiento WHERE nombre = ?", Integer.class, nuevo))
                .isZero();
    }

    @Test
    void listado_respetaElTamanoDePagina() {
        ResponseEntity<Map> r = llamar(HttpMethod.GET, URL + "?size=2&page=0", tokenAdmin(), null, Map.class);
        assertThat((List) r.getBody().get("contenido")).hasSize(2);
        assertThat(((Number) r.getBody().get("totalElementos")).intValue()).isGreaterThan(2);
    }
}
