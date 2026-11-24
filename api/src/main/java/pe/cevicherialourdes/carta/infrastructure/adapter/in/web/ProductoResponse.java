package pe.cevicherialourdes.carta.infrastructure.adapter.in.web;

import java.math.BigDecimal;
import java.util.List;

/** {@code precio} no aparece en las bebidas: su precio esta en cada presentacion. */
public record ProductoResponse(Long id, String nombre, String categoria, BigDecimal precio, boolean requiereCocina,
                               boolean disponible, boolean activo, List<PresentacionResponse> presentaciones) {

    /** Solo las presentaciones activas, de la mas barata a la mas cara. */
    public record PresentacionResponse(Long id, String nombre, BigDecimal precio) {
    }
}
