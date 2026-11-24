package pe.cevicherialourdes.identidad.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import pe.cevicherialourdes.shared.domain.exception.ReglaNegocioException;

/**
 * Persona que usa el sistema. Concentra la politica de bloqueo de HU-01:
 * 5 PIN fallidos seguidos bloquean el acceso 5 minutos.
 */
public class Usuario {

    public static final int MAX_INTENTOS_FALLIDOS = 5;
    public static final Duration DURACION_BLOQUEO = Duration.ofMinutes(5);
    private static final Pattern USERNAME = Pattern.compile("[a-z0-9._-]{3,30}");
    private static final int NOMBRE_MAX = 60;

    private final Long id;
    private final String nombre;
    private final String username;
    private final String pinHash;
    private final Rol rol;
    private final boolean activo;
    private int intentosFallidos;
    private Instant bloqueadoHasta;

    private Usuario(Long id, String nombre, String username, String pinHash, Rol rol, boolean activo,
                    int intentosFallidos, Instant bloqueadoHasta) {
        this.id = id;
        this.nombre = nombre;
        this.username = username;
        this.pinHash = Objects.requireNonNull(pinHash, "pinHash");
        this.rol = Objects.requireNonNull(rol, "rol");
        this.activo = activo;
        this.intentosFallidos = intentosFallidos;
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public static Usuario nuevo(String nombre, String username, String pinHash, Rol rol) {
        return new Usuario(null, validarNombre(nombre), normalizarUsername(username), pinHash, rol, true, 0, null);
    }

    /** Solo para adaptadores de persistencia: rehidrata un usuario ya validado. */
    public static Usuario reconstituir(Long id, String nombre, String username, String pinHash, Rol rol,
                                       boolean activo, int intentosFallidos, Instant bloqueadoHasta) {
        return new Usuario(Objects.requireNonNull(id, "id"), nombre, username, pinHash, rol, activo,
                intentosFallidos, bloqueadoHasta);
    }

    /** Forma canonica para buscar: "Mama" y "mama " son el mismo usuario. No valida (un login nunca da 422). */
    public static String claveDeBusqueda(String username) {
        return username == null ? "" : username.strip().toLowerCase(Locale.ROOT);
    }

    public static String normalizarUsername(String username) {
        String normalizado = claveDeBusqueda(username);
        if (!USERNAME.matcher(normalizado).matches()) {
            throw new ReglaNegocioException("USERNAME_INVALIDO",
                    "El usuario debe tener de 3 a 30 caracteres: letras, numeros, punto, guion o guion bajo");
        }
        return normalizado;
    }

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.strip().length() > NOMBRE_MAX) {
            throw new ReglaNegocioException("NOMBRE_INVALIDO", "El nombre es obligatorio (maximo 60 caracteres)");
        }
        return nombre.strip();
    }

    public boolean estaBloqueado(Instant ahora) {
        return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
    }

    public long minutosRestantesDeBloqueo(Instant ahora) {
        if (!estaBloqueado(ahora)) {
            return 0;
        }
        long segundos = Duration.between(ahora, bloqueadoHasta).toSeconds();
        return (segundos + 59) / 60;
    }

    public void registrarFallo(Instant ahora) {
        if (estaBloqueado(ahora)) {
            throw new IllegalStateException("No se cuentan intentos mientras el usuario esta bloqueado");
        }
        if (bloqueadoHasta != null) {
            bloqueadoHasta = null;      // el bloqueo anterior ya vencio: se empieza de cero
            intentosFallidos = 0;
        }
        intentosFallidos++;
        if (intentosFallidos >= MAX_INTENTOS_FALLIDOS) {
            bloqueadoHasta = ahora.plus(DURACION_BLOQUEO);
            intentosFallidos = 0;
        }
    }

    public void registrarAccesoExitoso() {
        intentosFallidos = 0;
        bloqueadoHasta = null;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUsername() {
        return username;
    }

    public String getPinHash() {
        return pinHash;
    }

    public Rol getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public Instant getBloqueadoHasta() {
        return bloqueadoHasta;
    }
}
