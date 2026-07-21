package com.hotel.paraiso.facturacion;

import com.hotel.paraiso.common.mapper.MapperCentralConfig;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Solo mapeo hacia Response: la construcción de la entidad Factura la hace
 * FacturaService (cálculo de totales y numeración por secuencia).
 */
@Mapper(config = MapperCentralConfig.class, builder = @Builder(disableBuilder = true))
public interface FacturaMapper {

    @Mapping(target = "reservaId", source = "reserva.id")
    @Mapping(target = "codigoReserva", source = "reserva.codigoReserva")
    FacturaResponse toResponse(Factura entity);
}
