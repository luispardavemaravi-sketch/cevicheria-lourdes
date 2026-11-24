package pe.cevicherialourdes.carta.infrastructure.adapter.in.web;

import java.math.BigDecimal;
import java.util.List;

/** {@code precio} no aparece en las bebidas: su precio esta en cada presentacion. */
public record ProductoResponse(Long id, String nombre, String categoria, BigDecimal precio, boolean requiereCocina,
                               boolean disponible, boolean activo, List<PresentacionResponse> presentaciones,
                               List<AcompanamientoResponse> acompanamientos) {

    /** Solo las presentaciones activas, de la mas barata a la mas cara. */
    public record PresentacionResponse(Long id, String nombre, BigDecimal precio) {
    }

    /** Sin precio (RN-01). El id es el que se usara para quitarlo de un pedido (HU-09). */
    public record AcompanamientoResponse(Long id, String nombre) {
    }
}
