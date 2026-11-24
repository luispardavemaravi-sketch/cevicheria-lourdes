package pe.cevicherialourdes.carta.infrastructure.adapter.in.web;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PresentacionRequest(
        @Schema(example = "1.5 L") @NotBlank @Size(max = 30) String nombre,
        @Schema(example = "9.50") @NotNull @DecimalMin("0.01") @Digits(integer = 6, fraction = 2) BigDecimal precio) {
}
