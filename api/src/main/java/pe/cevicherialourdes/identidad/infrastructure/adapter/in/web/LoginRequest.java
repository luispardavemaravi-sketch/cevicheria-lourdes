package pe.cevicherialourdes.identidad.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(example = "mama")
        @NotBlank @Size(max = 30) String username,

        @Schema(example = "1234", description = "PIN de 4 digitos")
        @NotNull @Pattern(regexp = "[0-9]{4}", message = "debe tener exactamente 4 digitos") String pin) {

    /** El PIN nunca debe aparecer en logs, ni siquiera si alguien imprime el request. */
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", pin=****]";
    }
}
