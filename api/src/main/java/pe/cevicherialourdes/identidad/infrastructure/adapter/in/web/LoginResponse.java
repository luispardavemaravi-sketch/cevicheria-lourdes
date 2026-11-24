package pe.cevicherialourdes.identidad.infrastructure.adapter.in.web;

import java.time.Instant;

public record LoginResponse(String token, String tipo, Instant expiraEn, Long usuarioId, String nombre, String rol) {
}
