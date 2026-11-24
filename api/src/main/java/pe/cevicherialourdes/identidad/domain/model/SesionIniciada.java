package pe.cevicherialourdes.identidad.domain.model;

import java.time.Instant;

public record SesionIniciada(String token, Instant expiraEn, Long usuarioId, String nombre, Rol rol) {
}
