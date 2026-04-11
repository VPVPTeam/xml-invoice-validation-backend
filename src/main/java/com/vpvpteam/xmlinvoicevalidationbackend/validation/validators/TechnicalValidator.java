package com.vpvpteam.xmlinvoicevalidationbackend.validation.validators;

import com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.dto.KsefInvoiceXmlDto;

/**
 * Interface for technical validators.
 * Implementations validate invoice DTO and return validation output.
 */
public interface TechnicalValidator {

    /**
     * Runs technical validation for invoice DTO.
     */
    TechnicalValidationOutput validate(KsefInvoiceXmlDto dto, String xmlFileName);
}