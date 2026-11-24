package pe.cevicherialourdes.carta.domain.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pe.cevicherialourdes.shared.domain.exception.ReglaNegocioException;
import pe.cevicherialourdes.shared.domain.model.Dinero;

/**
 * Producto de la carta.
 * <ul>
 *   <li>HU-03: nombre unico (lo verifica el caso de uso), precio &gt; 0, categoria inmutable, baja logica.</li>
 *   <li>HU-04: las BEBIDAS no tienen precio propio; lo tiene cada presentacion (al menos una activa).</li>
 *   <li>HU-05: solo los PLATOS tienen acompanamientos (sin precio).</li>
 * </ul>
 */
public class Producto {

    private static final int NOMBRE_MAX = 60;
    /** NUMERIC(8,2) en la base. */
    private static final Dinero PRECIO_MAXIMO = Dinero.de("999999.99");
    private static final Comparator<Presentacion> POR_PRECIO =
            Comparator.comparing(Presentacion::getPrecio).thenComparing(Presentacion::getNombre);

    private final Long id;
    private String nombre;
    private final Categoria categoria;
    private Dinero precio;
    private boolean requiereCocina;
    private final boolean disponible;
    private boolean activo;
    /** Todas, incluidas las inactivas (se conservan por los pedidos viejos). */
    private final List<Presentacion> presentaciones;
    /** HU-05: solo en PLATO; ordenados por nombre. */
    private List<Acompanamiento> acompanamientos;

    private Producto(Long id, String nombre, Categoria categoria, Dinero precio, boolean requiereCocina,
                     boolean disponible, boolean activo, List<Presentacion> presentaciones,
                     List<Acompanamiento> acompanamientos) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = Objects.requireNonNull(categoria, "categoria");
        this.precio = precio;
        this.requiereCocina = requiereCocina;
        this.disponible = disponible;
        this.activo = activo;
        this.presentaciones = new ArrayList<>(presentaciones);
        this.acompanamientos = ordenados(acompanamientos);
    }

    /**
     * @param requiereCocina null = el valor por defecto de la categoria (RN-06)
     * @param presentaciones obligatorias (al menos una) en BEBIDA; prohibidas en el resto
     */
    public static Producto crear(String nombre, Categoria categoria, Dinero precio, Boolean requiereCocina,
                                 List<DefinicionPresentacion> presentaciones) {
        Objects.requireNonNull(categoria, "categoria");
        String nombreValido = normalizarNombre(nombre);
        validarPrecio(categoria, precio);
        validarPresentaciones(categoria, presentaciones);
        Producto producto = new Producto(null, nombreValido, categoria, precio,
                requiereCocina != null ? requiereCocina : categoria.requiereCocinaPorDefecto(), true, true, List.of(),
                List.of());
        producto.aplicarPresentaciones(presentaciones);
        return producto;
    }

    public static Producto reconstituir(Long id, String nombre, Categoria categoria, Dinero precio,
                                        boolean requiereCocina, boolean disponible, boolean activo,
                                        List<Presentacion> presentaciones,
                                        List<Acompanamiento> acompanamientos) {
        return new Producto(Objects.requireNonNull(id, "id"), nombre, categoria, precio, requiereCocina,
                disponible, activo, presentaciones, acompanamientos);
    }

    /**
     * HU-05: reemplaza los acompanamientos del plato (ya resueltos en el catalogo). Solo un PLATO puede tenerlos.
     */
    public void asignarAcompanamientos(List<Acompanamiento> nuevos) {
        List<Acompanamiento> lista = nuevos == null ? List.of() : nuevos;
        if (!lista.isEmpty() && categoria != Categoria.PLATO) {
            throw new ReglaNegocioException("ACOMPANAMIENTO_SOLO_EN_PLATO", "Solo los platos tienen acompanamientos");
        }
        Set<Long> ids = new HashSet<>();
        for (Acompanamiento a : lista) {
            if (!ids.add(a.id())) {
                throw new ReglaNegocioException("ACOMPANAMIENTO_DUPLICADO",
                        "El acompanamiento '" + a.nombre() + "' esta repetido");
            }
        }
        this.acompanamientos = ordenados(lista);
    }

    private static List<Acompanamiento> ordenados(List<Acompanamiento> lista) {
        return lista.stream().sorted(Comparator.comparing(Acompanamiento::nombre)).toList();
    }

    /**
     * Reemplaza los datos del producto. Para bebidas, {@code nuevasPresentaciones} es la lista completa deseada:
     * las existentes se emparejan por nombre (sin mayusculas) y conservan su id; las nuevas se agregan;
     * las que no vienen quedan inactivas. Todo se valida antes de modificar nada.
     */
    public void actualizar(String nuevoNombre, Categoria nuevaCategoria, Dinero nuevoPrecio,
                           Boolean nuevoRequiereCocina, boolean nuevoActivo,
                           List<DefinicionPresentacion> nuevasPresentaciones) {
        if (nuevaCategoria != categoria) {
            throw new ReglaNegocioException("CATEGORIA_NO_MODIFICABLE",
                    "La categoria de un producto no se puede cambiar; crea otro producto");
        }
        String nombreValido = normalizarNombre(nuevoNombre);
        validarPrecio(categoria, nuevoPrecio);
        validarPresentaciones(categoria, nuevasPresentaciones);

        this.nombre = nombreValido;
        this.precio = nuevoPrecio;
        if (nuevoRequiereCocina != null) {
            this.requiereCocina = nuevoRequiereCocina;
        }
        this.activo = nuevoActivo;
        aplicarPresentaciones(nuevasPresentaciones);
    }

    /**
     * Precio unitario al pedir este producto (lo congela HU-07 en detalle_pedido, RN-02).
     * HU-04: pedir una bebida exige una presentacion activa de esta bebida.
     */
    public Dinero precioPara(Long presentacionId) {
        if (categoria.llevaPrecioPropio()) {
            if (presentacionId != null) {
                throw new ReglaNegocioException("PRESENTACION_NO_APLICA",
                        "'" + nombre + "' no tiene presentaciones");
            }
            return precio;
        }
        if (presentacionId == null) {
            throw new ReglaNegocioException("PRESENTACION_REQUERIDA",
                    "Para pedir '" + nombre + "' hay que elegir una presentacion");
        }
        return presentaciones.stream()
                .filter(p -> p.isActivo() && presentacionId.equals(p.getId()))
                .findFirst()
                .map(Presentacion::getPrecio)
                .orElseThrow(() -> new ReglaNegocioException("PRESENTACION_NO_VALIDA",
                        "La presentacion " + presentacionId + " no esta disponible para '" + nombre + "'"));
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

    private static void validarPresentaciones(Categoria categoria, List<DefinicionPresentacion> definiciones) {
        boolean vacia = definiciones == null || definiciones.isEmpty();
        if (categoria.llevaPrecioPropio()) {
            if (!vacia) {
                throw new ReglaNegocioException("PRESENTACION_SOLO_EN_BEBIDA",
                        "Solo las bebidas tienen presentaciones");
            }
            return;
        }
        if (vacia) {
            throw new ReglaNegocioException("PRESENTACION_REQUERIDA",
                    "Una bebida necesita al menos una presentacion con precio");
        }
        Set<String> vistas = new HashSet<>();
        for (DefinicionPresentacion d : definiciones) {
            Presentacion.validarNombre(d.nombre());
            Presentacion.validarPrecio(d.precio());
            if (!vistas.add(Presentacion.clave(d.nombre()))) {
                throw new ReglaNegocioException("PRESENTACION_DUPLICADA",
                        "La presentacion '" + d.nombre().strip() + "' esta repetida");
            }
        }
    }

    /** Precondicion: definiciones ya validadas. */
    private void aplicarPresentaciones(List<DefinicionPresentacion> definiciones) {
        if (definiciones == null) {
            return;
        }
        Set<String> deseadas = new HashSet<>();
        for (DefinicionPresentacion d : definiciones) {
            String clave = Presentacion.clave(d.nombre());
            deseadas.add(clave);
            presentaciones.stream()
                    .filter(p -> p.clave().equals(clave))
                    .findFirst()
                    .ifPresentOrElse(p -> p.redefinir(d),
                            () -> presentaciones.add(Presentacion.nueva(d.nombre(), d.precio())));
        }
        presentaciones.stream()
                .filter(p -> !deseadas.contains(p.clave()))
                .forEach(Presentacion::desactivar);
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

    /** Todas, incluidas las inactivas. */
    public List<Presentacion> getPresentaciones() {
        return List.copyOf(presentaciones);
    }

    /** Las que se pueden pedir, de la mas barata a la mas cara. */
    public List<Acompanamiento> getAcompanamientos() {
        return acompanamientos;
    }

    public List<Presentacion> presentacionesActivas() {
        return presentaciones.stream().filter(Presentacion::isActivo).sorted(POR_PRECIO).toList();
    }
}
