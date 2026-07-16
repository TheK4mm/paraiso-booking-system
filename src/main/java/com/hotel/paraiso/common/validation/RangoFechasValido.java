package com.hotel.paraiso.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida a nivel de clase que fechaSalida sea posterior a fechaEntrada.
 * El error se reporta sobre el campo fechaSalida para que el formulario
 * lo muestre junto al campo (th:errors).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RangoFechasValidator.class)
public @interface RangoFechasValido {

    String message() default "La fecha de salida debe ser posterior a la fecha de entrada";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
