package com.hotel.paraiso.empleado;

import com.hotel.paraiso.common.crud.CrudMapper;
import com.hotel.paraiso.common.mapper.MapperCentralConfig;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapperCentralConfig.class, builder = @Builder(disableBuilder = true))
public interface EmpleadoMapper extends CrudMapper<Empleado, EmpleadoRequest, EmpleadoResponse> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true")
    @Mapping(target = "reservas", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    @Mapping(target = "actualizadoEn", ignore = true)
    @Mapping(target = "creadoPor", ignore = true)
    @Mapping(target = "actualizadoPor", ignore = true)
    Empleado toEntity(EmpleadoRequest request);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "reservas", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    @Mapping(target = "actualizadoEn", ignore = true)
    @Mapping(target = "creadoPor", ignore = true)
    @Mapping(target = "actualizadoPor", ignore = true)
    void updateEntity(EmpleadoRequest request, @MappingTarget Empleado entity);

    @Override
    @Mapping(target = "nombreCompleto", expression = "java(entity.getNombre() + \" \" + entity.getApellido())")
    @Mapping(target = "totalReservasGestionadas", ignore = true)
    EmpleadoResponse toResponse(Empleado entity);
}
