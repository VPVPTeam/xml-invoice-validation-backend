package com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef;

import com.vpvpteam.xmlinvoicevalidationbackend.formats.FormatProcessor;
import com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.dto.KsefInvoiceXmlDto;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.technical.KsefTechnicalValidator;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.technical.TechnicalValidationOutput;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@AllArgsConstructor
public final class KsefFormatProcessor implements FormatProcessor {
    private final KsefInvoiceParser parser;
    private final KsefTechnicalValidator technicalValidator;

    @Override
    public String getFormatName() {
        return "KSEF";
    }

    @Override
    public TechnicalValidationOutput process(InputStream xmlInput, String fileName) {
        KsefInvoiceXmlDto dto = parser.parse(xmlInput);

        TechnicalValidationOutput output = technicalValidator.validate(dto, fileName);
        if (dto == null) {
            return output;
        }

        output.setSellerTaxId(dto.safeSellerTaxId());
        output.setInvoiceNumber(dto.safeInvoiceNumber());

        return output;
    }
}