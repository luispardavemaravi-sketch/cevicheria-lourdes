package pe.cevicherialourdes.identidad.infrastructure.adapter.in.web;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import pe.cevicherialourdes.identidad.domain.model.SesionIniciada;

@Mapper
public interface LoginWebMapper {

    @Mapping(target = "tipo", constant = "Bearer")
    LoginResponse aResponse(SesionIniciada sesion);
}
