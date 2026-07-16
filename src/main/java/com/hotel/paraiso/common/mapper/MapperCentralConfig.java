package com.hotel.paraiso.common.mapper;

import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

/**
 * Configuración compartida de MapStruct. unmappedTargetPolicy = ERROR
 * obliga a declarar explícitamente cada campo ignorado: si una entidad
 * o DTO gana un campo nuevo y el mapper no lo contempla, el build falla.
 */
@MapperConfig(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MapperCentralConfig {
}
