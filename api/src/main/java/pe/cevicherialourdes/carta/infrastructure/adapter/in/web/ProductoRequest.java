package pe.cevicherialourdes.carta.infrastructure.adapter.in.web;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.cevicherialourdes.carta.domain.model.Categoria;

public record ProductoRequest(
        @Schema(example = "Ceviche de pescado")
        @NotBlank @Size(max = 60) String nombre,

        @NotNull Categoria categoria,

        @Schema(example = "15.00", description = "Obligatorio en PLATO y TAPER; las BEBIDAS no llevan precio propio")
        @DecimalMin(value = "0.01") @Digits(integer = 6, fraction = 2) BigDecimal precio,

        @Schema(description = "Si se omite: true para PLATO, false para BEBIDA y TAPER (RN-06)")
        Boolean requiereCocina,

        @Schema(description = "Solo en PUT. false = baja logica. Si se omite: true")
        Boolean activo,

        @Schema(description = "Solo BEBIDA y obligatorio en ella (HU-04). En PUT es la lista completa: "
                + "las que no vengan quedan inactivas")
        @Size(max = 20) List<@Valid @NotNull PresentacionRequest> presentaciones,

        @Schema(example = "[\"Camote\", \"Cebolla\"]", description = "Solo PLATO (HU-05). Por nombre: si no existe "
                + "en el catalogo se crea. En PUT es la lista completa; si se omite, el plato queda sin acompanamientos")
        @Size(max = 30) List<@NotBlank @Size(max = 40) String> acompanamientos) {
}
