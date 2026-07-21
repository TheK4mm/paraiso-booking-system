package com.hotel.paraiso.servicio;

import com.hotel.paraiso.common.crud.CrudMapper;
import com.hotel.paraiso.common.mapper.MapperCentralConfig;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapperCentralConfig.class, builder = @Builder(disableBuilder = true))
public interface ServicioMapper extends CrudMapper<Servicio, ServicioRequest, ServicioResponse> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true")
    @Mapping(target = "reservas", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    @Mapping(target = "actualizadoEn", ignore = true)
    @Mapping(target = "creadoPor", ignore = true)
    @Mapping(target = "actualizadoPor", ignore = true)
    Servicio toEntity(ServicioRequest request);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "reservas", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    @Mapping(target = "actualizadoEn", ignore = true)
    @Mapping(target = "creadoPor", ignore = true)
    @Mapping(target = "actualizadoPor", ignore = true)
    void updateEntity(ServicioRequest request, @MappingTarget Servicio entity);

    @Override
    ServicioResponse toResponse(Servicio entity);
}
