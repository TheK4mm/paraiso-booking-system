package com.hotel.paraiso.facturacion;

import com.hotel.paraiso.common.mapper.MapperCentralConfig;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Solo mapeo hacia Response: la construcción de la entidad Pago la hace
 * PagoService (requiere validación de saldo bajo lock).
 */
@Mapper(config = MapperCentralConfig.class, builder = @Builder(disableBuilder = true))
public interface PagoMapper {

    @Mapping(target = "reservaId", source = "reserva.id")
    @Mapping(target = "codigoReserva", source = "reserva.codigoReserva")
    @Mapping(target = "facturaId", source = "factura.id")
    PagoResponse toResponse(Pago entity);
}
