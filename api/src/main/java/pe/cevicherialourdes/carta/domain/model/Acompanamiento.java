package pe.cevicherialourdes.carta.domain.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import pe.cevicherialourdes.shared.domain.exception.ReglaNegocioException;

/**
 * HU-05: guarnicion que trae un plato (camote, cebolla...). No tiene precio: quitarla no cambia el precio (RN-01).
 * Es un catalogo compartido entre platos; se identifica por nombre sin importar mayusculas.
 */
public record Acompanamiento(Long id, String nombre) {

    private static final int NOMBRE_MAX = 40;

    public Acompanamiento {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nombre, "nombre");
    }

    /** Forma de comparar nombres: "Camote" = " camote ". */
    public static String clave(String nombre) {
        return nombre.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /**
     * Valida y limpia los nombres que manda el ADMIN, conservando el orden.
     * @return lista vacia si {@code nombres} es null
     */
    public static List<String> normalizarNombres(List<String> nombres) {
        if (nombres == null) {
            return List.of();
        }
        List<String> limpios = new ArrayList<>();
        Set<String> vistas = new HashSet<>();
        for (String nombre : nombres) {
            String limpio = nombre == null ? "" : nombre.strip().replaceAll("\\s+", " ");
            if (limpio.isEmpty() || limpio.length() > NOMBRE_MAX) {
                throw new ReglaNegocioException("ACOMPANAMIENTO_INVALIDO",
                        "El nombre del acompanamiento es obligatorio (maximo 40 caracteres)");
            }
            if (!vistas.add(clave(limpio))) {
                throw new ReglaNegocioException("ACOMPANAMIENTO_DUPLICADO",
                        "El acompanamiento '" + limpio + "' esta repetido");
            }
            limpios.add(limpio);
        }
        return limpios;
    }
}
