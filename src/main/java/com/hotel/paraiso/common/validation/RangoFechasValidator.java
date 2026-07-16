package com.hotel.paraiso.common.validation;

import com.hotel.paraiso.reserva.ReservaRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RangoFechasValidator implements ConstraintValidator<RangoFechasValido, ReservaRequest> {

    @Override
    public boolean isValid(ReservaRequest request, ConstraintValidatorContext context) {
        if (request.getFechaEntrada() == null || request.getFechaSalida() == null) {
            return true; // @NotNull de cada campo reporta la ausencia
        }
        if (request.getFechaSalida().isAfter(request.getFechaEntrada())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("fechaSalida")
                .addConstraintViolation();
        return false;
    }
}
