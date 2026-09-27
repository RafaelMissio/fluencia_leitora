package com.missio.fluencia_leitora.cadastros.anoletivo.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** CAD-03: dataFim deve ser posterior a dataInicio. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DataFimPosteriorADataInicioValidator.class)
public @interface DataFimPosteriorADataInicio {

    String message() default "dataFim deve ser posterior a dataInicio";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
