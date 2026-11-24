package pe.cevicherialourdes.carta.domain.model;

import java.util.Objects;

import pe.cevicherialourdes.shared.domain.exception.ReglaNegocioException;
import pe.cevicherialourdes.shared.domain.model.Dinero;

/**
 * Producto de la carta. HU-03: nombre unico (lo verifica el caso de uso), precio &gt; 0.
 * La categoria no cambia despues de creada. La baja es logica (activo = false): los pedidos viejos lo referencian.
 */
public class Producto {

    private static final int NOMBRE_MAX = 60;
    /** NUMERIC(8,2) en la base. */
    private static final Dinero PRECIO_MAXIMO = Dinero.de("999999.99");

    private final Long id;
    private String nombre;
    private final Categoria categoria;
    private Dinero precio;
    private boolean requiereCocina;
    private final boolean disponible;
    private boolean activo;

    private Producto(Long id, String nombre, Categoria categoria, Dinero precio, boolean requiereCocina,
                     boolean disponible, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = Objects.requireNonNull(categoria, "categoria");
        this.precio = precio;
        this.requiereCocina = requiereCocina;
        this.disponible = disponible;
        this.activo = activo;
    }

    /** @param requiereCocina null = el valor por defecto de la categoria (RN-06) */
    public static Producto crear(String nombre, Categoria categoria, Dinero precio, Boolean requiereCocina) {
        Objects.requireNonNull(categoria, "categoria");
        validarPrecio(categoria, precio);
        return new Producto(null, normalizarNombre(nombre), categoria, precio,
                requiereCocina != null ? requiereCocina : categoria.requiereCocinaPorDefecto(), true, true);
    }

    public static Producto reconstituir(Long id, String nombre, Categoria categoria, Dinero precio,
                                        boolean requiereCocina, boolean disponible, boolean activo) {
        return new Producto(Objects.requireNonNull(id, "id"), nombre, categoria, precio, requiereCocina,
                disponible, activo);
    }

    public void actualizar(String nuevoNombre, Categoria nuevaCategoria, Dinero nuevoPrecio,
                           Boolean nuevoRequiereCocina, boolean nuevoActivo) {
        if (nuevaCategoria != categoria) {
            throw new ReglaNegocioException("CATEGORIA_NO_MODIFICABLE",
                    "La categoria de un producto no se puede cambiar; crea otro producto");
        }
        validarPrecio(categoria, nuevoPrecio);
        String nombreValido = normalizarNombre(nuevoNombre);
        this.nombre = nombreValido;
        this.precio = nuevoPrecio;
        if (nuevoRequiereCocina != null) {
            this.requiereCocina = nuevoRequiereCocina;
        }
        this.activo = nuevoActivo;
    }

    /** Recorta y colapsa espacios: "  Ceviche   de pescado " y "Ceviche de pescado" son el mismo nombre. */
    public static String normalizarNombre(String nombre) {
        String limpio = nombre == null ? "" : nombre.strip().replaceAll("\\s+", " ");
        if (limpio.isEmpty() || limpio.length() > NOMBRE_MAX) {
            throw new ReglaNegocioException("NOMBRE_INVALIDO", "El nombre es obligatorio (maximo 60 caracteres)");
        }
        return limpio;
    }

    private static void validarPrecio(Categoria categoria, Dinero precio) {
        if (!categoria.llevaPrecioPropio()) {
            if (precio != null) {
                throw new ReglaNegocioException("PRECIO_EN_PRESENTACION",
                        "Las bebidas no llevan precio propio: el precio va en cada presentacion");
            }
            return;
        }
        if (precio == null || !precio.esPositivo() || precio.esMayorQue(PRECIO_MAXIMO)) {
            throw new ReglaNegocioException("PRECIO_INVALIDO", "El precio debe ser mayor que 0 (maximo 999999.99)");
        }
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    /** null en bebidas: su precio esta en cada presentacion. */
    public Dinero getPrecio() {
        return precio;
    }

    public boolean isRequiereCocina() {
        return requiereCocina;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public boolean isActivo() {
        return activo;
    }
}
