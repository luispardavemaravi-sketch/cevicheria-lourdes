package pe.cevicherialourdes.carta.infrastructure.adapter.in.web;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import pe.cevicherialourdes.carta.domain.model.Categoria;
import pe.cevicherialourdes.carta.domain.model.Producto;
import pe.cevicherialourdes.carta.domain.port.in.GestionarProductosUseCase;
import pe.cevicherialourdes.carta.domain.port.in.GestionarProductosUseCase.FiltroProductos;
import pe.cevicherialourdes.shared.domain.model.Paginacion;
import pe.cevicherialourdes.shared.infrastructure.adapter.in.web.PaginaResponse;

@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "Carta")
public class ProductoController {

    private final GestionarProductosUseCase productos;
    private final ProductoWebMapper mapper;

    public ProductoController(GestionarProductosUseCase productos, ProductoWebMapper mapper) {
        this.productos = productos;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar la carta [HU-03]",
            description = "Rol: cualquier usuario autenticado. Ordenado por nombre; maximo 100 por pagina.")
    public PaginaResponse<ProductoResponse> listar(
            @RequestParam(required = false) Categoria categoria,
            @Parameter(description = "true = incluye los dados de baja")
            @RequestParam(defaultValue = "false") boolean incluirInactivos,
            @Parameter(description = "Pagina, base 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamano de pagina, maximo 100") @RequestParam(defaultValue = "50") int size) {
        var pagina = productos.listar(new FiltroProductos(categoria, incluirInactivos), new Paginacion(page, size));
        return PaginaResponse.de(pagina, mapper::aResponse);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear producto [HU-03]",
            description = "Rol: ADMIN. 409 PRODUCTO_DUPLICADO; 422 PRECIO_INVALIDO, PRECIO_EN_PRESENTACION.")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        Producto creado = productos.crear(mapper.aDatos(request));
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(creado.getId()).toUri();
        return ResponseEntity.created(ubicacion).body(mapper.aResponse(creado));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Editar o dar de baja un producto [HU-03]",
            description = "Rol: ADMIN. activo=false es la baja logica. 404 PRODUCTO_NO_ENCONTRADO; "
                    + "409 PRODUCTO_DUPLICADO; 422 CATEGORIA_NO_MODIFICABLE, PRECIO_INVALIDO.")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return mapper.aResponse(productos.actualizar(id, mapper.aDatos(request)));
    }
}
