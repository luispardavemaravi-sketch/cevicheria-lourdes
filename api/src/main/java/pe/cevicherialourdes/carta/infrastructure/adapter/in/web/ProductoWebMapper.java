package pe.cevicherialourdes.carta.infrastructure.adapter.in.web;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import pe.cevicherialourdes.carta.domain.model.Producto;
import pe.cevicherialourdes.carta.domain.port.in.GestionarProductosUseCase.DatosProducto;

@Mapper
public interface ProductoWebMapper {

    DatosProducto aDatos(ProductoRequest request);

    @Mapping(target = "precio", source = "precio.monto")
    ProductoResponse aResponse(Producto producto);
}
