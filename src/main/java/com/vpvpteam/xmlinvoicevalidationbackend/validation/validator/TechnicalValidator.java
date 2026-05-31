package com.vpvpteam.xmlinvoicevalidationbackend.validation.validator;

import com.vpvpteam.xmlinvoicevalidationbackend.formats.InvoiceXmlDto;

/**
 * Interface for technical validators.
 * Implementations validate invoice DTO and return validation output.
 */
public interface TechnicalValidator<T extends InvoiceXmlDto> {
    /**
     * Runs technical validation for invoice DTO.
     */
    TechnicalValidationOutput validate(T dto, String xmlFileName);
}