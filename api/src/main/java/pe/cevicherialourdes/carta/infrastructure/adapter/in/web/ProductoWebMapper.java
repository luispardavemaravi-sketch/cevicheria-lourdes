package pe.cevicherialourdes.carta.infrastructure.adapter.in.web;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import pe.cevicherialourdes.carta.domain.model.Presentacion;
import pe.cevicherialourdes.carta.domain.model.Producto;
import pe.cevicherialourdes.carta.domain.port.in.GestionarProductosUseCase.DatosProducto;
import pe.cevicherialourdes.carta.infrastructure.adapter.in.web.ProductoResponse.PresentacionResponse;

@Mapper
public interface ProductoWebMapper {

    DatosProducto aDatos(ProductoRequest request);

    @Mapping(target = "precio", source = "precio.monto")
    @Mapping(target = "presentaciones", expression = "java(producto.presentacionesActivas().stream()"
            + ".map(this::aResponse).toList())")
    ProductoResponse aResponse(Producto producto);

    @Mapping(target = "precio", source = "precio.monto")
    PresentacionResponse aResponse(Presentacion presentacion);
}
