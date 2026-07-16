package com.hotel.paraiso.reserva;

import com.hotel.paraiso.common.mapper.MapperCentralConfig;
import com.hotel.paraiso.facturacion.FacturaMapper;
import com.hotel.paraiso.facturacion.PagoMapper;
import com.hotel.paraiso.habitacion.HabitacionMapper;
import com.hotel.paraiso.servicio.ServicioMapper;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Solo mapeo hacia Response: la construcción de la entidad Reserva la hace
 * ReservaService (disponibilidad bajo lock, precios, máquina de estados).
 */
@Mapper(config = MapperCentralConfig.class,
        builder = @Builder(disableBuilder = true),
        uses = {HabitacionMapper.class, ServicioMapper.class, PagoMapper.class, FacturaMapper.class})
public interface ReservaMapper {

    @Mapping(target = "clienteId", source = "cliente.id")
    @Mapping(target = "clienteNombreCompleto",
            expression = "java(entity.getCliente().getNombre() + \" \" + entity.getCliente().getApellido())")
    @Mapping(target = "clienteDocumento", source = "cliente.numeroDocumento")
    @Mapping(target = "empleadoId", source = "empleado.id")
    @Mapping(target = "empleadoNombreCompleto",
            expression = "java(entity.getEmpleado() != null ? entity.getEmpleado().getNombre() + \" \" + entity.getEmpleado().getApellido() : null)")
    ReservaResponse toResponse(Reserva entity);

    @Mapping(target = "clienteId", source = "cliente.id")
    @Mapping(target = "clienteNombreCompleto",
            expression = "java(entity.getCliente().getNombre() + \" \" + entity.getCliente().getApellido())")
    @Mapping(target = "clienteDocumento", source = "cliente.numeroDocumento")
    @Mapping(target = "empleadoId", source = "empleado.id")
    @Mapping(target = "empleadoNombreCompleto",
            expression = "java(entity.getEmpleado() != null ? entity.getEmpleado().getNombre() + \" \" + entity.getEmpleado().getApellido() : null)")
    @Mapping(target = "totalPagado", ignore = true)
    @Mapping(target = "saldoPendiente", ignore = true)
    ReservaDetalleResponse toDetalle(Reserva entity);
}
