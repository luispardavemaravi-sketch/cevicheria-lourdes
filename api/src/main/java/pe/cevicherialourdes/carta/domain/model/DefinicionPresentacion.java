package pe.cevicherialourdes.carta.domain.model;

import pe.cevicherialourdes.shared.domain.model.Dinero;

/** Como quiere el ADMIN que quede una presentacion (nombre + precio) al crear o editar una bebida. */
public record DefinicionPresentacion(String nombre, Dinero precio) {
}
