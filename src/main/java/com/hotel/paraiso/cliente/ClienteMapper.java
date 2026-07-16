package com.hotel.paraiso.cliente;

import com.hotel.paraiso.common.crud.CrudMapper;
import com.hotel.paraiso.common.mapper.MapperCentralConfig;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapperCentralConfig.class, builder = @Builder(disableBuilder = true))
public interface ClienteMapper extends CrudMapper<Cliente, ClienteRequest, ClienteResponse> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true")
    @Mapping(target = "reservas", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    @Mapping(target = "actualizadoEn", ignore = true)
    @Mapping(target = "creadoPor", ignore = true)
    @Mapping(target = "actualizadoPor", ignore = true)
    Cliente toEntity(ClienteRequest request);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "reservas", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    @Mapping(target = "actualizadoEn", ignore = true)
    @Mapping(target = "creadoPor", ignore = true)
    @Mapping(target = "actualizadoPor", ignore = true)
    void updateEntity(ClienteRequest request, @MappingTarget Cliente entity);

    @Override
    @Mapping(target = "nombreCompleto", expression = "java(entity.getNombre() + \" \" + entity.getApellido())")
    @Mapping(target = "totalReservas", ignore = true)
    ClienteResponse toResponse(Cliente entity);
}
