package com.vpvpteam.xmlinvoicevalidationbackend.formats;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.TechnicalValidationOutput;

import java.io.InputStream;

public interface FormatProcessor {
    String getFormatName();

    TechnicalValidationOutput process(InputStream xmlInput, String fileName);
}