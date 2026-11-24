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

    @Test
    void listado_respetaElTamanoDePagina() {
        ResponseEntity<Map> r = llamar(HttpMethod.GET, URL + "?size=2&page=0", tokenAdmin(), null, Map.class);
        assertThat((List) r.getBody().get("contenido")).hasSize(2);
        assertThat(((Number) r.getBody().get("totalElementos")).intValue()).isGreaterThan(2);
    }
}
