package pe.cevicherialourdes.carta.infrastructure.adapter.in.web;

import java.math.BigDecimal;

public record ProductoResponse(Long id, String nombre, String categoria, BigDecimal precio, boolean requiereCocina,
                               boolean disponible, boolean activo) {
}
