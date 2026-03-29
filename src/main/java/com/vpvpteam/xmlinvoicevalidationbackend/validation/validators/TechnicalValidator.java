package com.vpvpteam.xmlinvoicevalidationbackend.validation.validators;
import com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.dto.KsefInvoiceXmlDto;

public interface TechnicalValidator {
    TechnicalValidationOutput validate(KsefInvoiceXmlDto dto);
}