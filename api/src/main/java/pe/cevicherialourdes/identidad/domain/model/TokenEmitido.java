package pe.cevicherialourdes.identidad.domain.model;

import java.time.Instant;

public record TokenEmitido(String valor, Instant expiraEn) {
}
