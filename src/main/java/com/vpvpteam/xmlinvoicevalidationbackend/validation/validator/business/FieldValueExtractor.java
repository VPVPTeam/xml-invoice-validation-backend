package com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.business;

import com.vpvpteam.xmlinvoicevalidationbackend.canonical.CanonicalFieldRegistry;
import com.vpvpteam.xmlinvoicevalidationbackend.canonical.CanonicalInvoice;
import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.UnsupportedFieldPathException;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public final class FieldValueExtractor {
    public String extract(CanonicalInvoice invoice, String fieldPath) throws UnsupportedFieldPathException {
        Function<CanonicalInvoice, String> extractor = CanonicalFieldRegistry.getExtractor(fieldPath);

        if (extractor == null) {
            throw new UnsupportedFieldPathException(fieldPath);
        }

        try {
            return extractor.apply(invoice);
        } catch (NullPointerException e) {
            return null;
        }
    }
}