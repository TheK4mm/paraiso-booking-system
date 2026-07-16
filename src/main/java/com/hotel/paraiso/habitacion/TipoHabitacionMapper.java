package com.hotel.paraiso.habitacion;

import com.hotel.paraiso.common.crud.CrudMapper;
import com.hotel.paraiso.common.mapper.MapperCentralConfig;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapperCentralConfig.class, builder = @Builder(disableBuilder = true))
public interface TipoHabitacionMapper extends CrudMapper<TipoHabitacion, TipoHabitacionRequest, TipoHabitacionResponse> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true")
    @Mapping(target = "habitaciones", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    @Mapping(target = "actualizadoEn", ignore = true)
    @Mapping(target = "creadoPor", ignore = true)
    @Mapping(target = "actualizadoPor", ignore = true)
    TipoHabitacion toEntity(TipoHabitacionRequest request);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "habitaciones", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    @Mapping(target = "actualizadoEn", ignore = true)
    @Mapping(target = "creadoPor", ignore = true)
    @Mapping(target = "actualizadoPor", ignore = true)
    void updateEntity(TipoHabitacionRequest request, @MappingTarget TipoHabitacion entity);

    @Override
    @Mapping(target = "totalHabitaciones", ignore = true)
    TipoHabitacionResponse toResponse(TipoHabitacion entity);
}
