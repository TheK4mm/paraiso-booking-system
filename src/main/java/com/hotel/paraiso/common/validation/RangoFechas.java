package com.hotel.paraiso.common.validation;

import java.time.LocalDate;

/**
 * Contrato para DTOs con rango entrada/salida validable por
 * {@link RangoFechasValido}. Los getters los genera Lombok.
 */
public interface RangoFechas {

    LocalDate getFechaEntrada();

    LocalDate getFechaSalida();
}
