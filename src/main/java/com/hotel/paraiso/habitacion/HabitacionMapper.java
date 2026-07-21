package com.hotel.paraiso.habitacion;

import com.hotel.paraiso.common.crud.CrudMapper;
import com.hotel.paraiso.common.mapper.MapperCentralConfig;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapperCentralConfig.class, builder = @Builder(disableBuilder = true))
public interface HabitacionMapper extends CrudMapper<Habitacion, HabitacionRequest, HabitacionResponse> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", source = "estado", defaultValue = "DISPONIBLE")
    @Mapping(target = "activo", constant = "true")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "tipoHabitacion", ignore = true) // se resuelve en applyRelations
    @Mapping(target = "reservas", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    @Mapping(target = "actualizadoEn", ignore = true)
    @Mapping(target = "creadoPor", ignore = true)
    @Mapping(target = "actualizadoPor", ignore = true)
    Habitacion toEntity(HabitacionRequest request);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", source = "estado", defaultValue = "DISPONIBLE")
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "tipoHabitacion", ignore = true)
    @Mapping(target = "reservas", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    @Mapping(target = "actualizadoEn", ignore = true)
    @Mapping(target = "creadoPor", ignore = true)
    @Mapping(target = "actualizadoPor", ignore = true)
    void updateEntity(HabitacionRequest request, @MappingTarget Habitacion entity);

    @Override
    @Mapping(target = "tipoHabitacionId", source = "tipoHabitacion.id")
    @Mapping(target = "tipoHabitacionNombre", source = "tipoHabitacion.nombre")
    @Mapping(target = "precioBaseNoche", source = "tipoHabitacion.precioBaseNoche")
    @Mapping(target = "capacidadMaxima", source = "tipoHabitacion.capacidadMaxima")
    HabitacionResponse toResponse(Habitacion entity);
}
