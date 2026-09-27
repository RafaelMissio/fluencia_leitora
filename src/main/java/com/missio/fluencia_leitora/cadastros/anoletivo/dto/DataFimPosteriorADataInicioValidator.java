package com.missio.fluencia_leitora.cadastros.anoletivo.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DataFimPosteriorADataInicioValidator
        implements ConstraintValidator<DataFimPosteriorADataInicio, CriarAnoLetivoRequest> {

    @Override
    public boolean isValid(CriarAnoLetivoRequest request, ConstraintValidatorContext context) {
        if (request == null || request.dataInicio() == null || request.dataFim() == null) {
            return true;
        }
        if (request.dataFim().isAfter(request.dataInicio())) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("dataFim")
                .addConstraintViolation();
        return false;
    }
}
