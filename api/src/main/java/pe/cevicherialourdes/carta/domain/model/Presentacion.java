package pe.cevicherialourdes.carta.domain.model;

import java.util.Locale;
import java.util.Objects;

import pe.cevicherialourdes.shared.domain.exception.ReglaNegocioException;
import pe.cevicherialourdes.shared.domain.model.Dinero;

/**
 * HU-04: tamano de una bebida con precio propio (Personal, 1 L...). Nunca se borra: los pedidos viejos la
 * referencian, asi que cuando el ADMIN la quita queda inactiva.
 */
public class Presentacion {

    private static final int NOMBRE_MAX = 30;
    private static final Dinero PRECIO_MAXIMO = Dinero.de("999999.99");

    private final Long id;
    private String nombre;
    private Dinero precio;
    private boolean activo;

    private Presentacion(Long id, String nombre, Dinero precio, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.activo = activo;
    }

    public static Presentacion nueva(String nombre, Dinero precio) {
        return new Presentacion(null, validarNombre(nombre), validarPrecio(precio), true);
    }

    public static Presentacion reconstituir(Long id, String nombre, Dinero precio, boolean activo) {
        return new Presentacion(Objects.requireNonNull(id, "id"), nombre, precio, activo);
    }

    static String clave(String nombre) {
        return nombre == null ? "" : nombre.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    String clave() {
        return clave(nombre);
    }

    /** Aplica la definicion (validada antes por Producto) y la deja activa. */
    void redefinir(DefinicionPresentacion definicion) {
        this.nombre = validarNombre(definicion.nombre());
        this.precio = validarPrecio(definicion.precio());
        this.activo = true;
    }

    void desactivar() {
        this.activo = false;
    }

    static String validarNombre(String nombre) {
        String limpio = nombre == null ? "" : nombre.strip().replaceAll("\\s+", " ");
        if (limpio.isEmpty() || limpio.length() > NOMBRE_MAX) {
            throw new ReglaNegocioException("PRESENTACION_INVALIDA",
                    "El nombre de la presentacion es obligatorio (maximo 30 caracteres)");
        }
        return limpio;
    }

    static Dinero validarPrecio(Dinero precio) {
        if (precio == null || !precio.esPositivo() || precio.esMayorQue(PRECIO_MAXIMO)) {
            throw new ReglaNegocioException("PRECIO_INVALIDO",
                    "El precio de la presentacion debe ser mayor que 0 (maximo 999999.99)");
        }
        return precio;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Dinero getPrecio() {
        return precio;
    }

    public boolean isActivo() {
        return activo;
    }
}
