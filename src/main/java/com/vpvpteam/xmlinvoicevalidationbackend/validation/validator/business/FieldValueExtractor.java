package com.vpvpteam.xmlinvoicevalidationbackend.validation.validator;

import com.vpvpteam.xmlinvoicevalidationbackend.canonical.CanonicalFieldRegistry;
import com.vpvpteam.xmlinvoicevalidationbackend.canonical.CanonicalInvoice;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public final class FieldValueExtractor {
    public String extract(CanonicalInvoice invoice, String fieldPath) {
        Function<CanonicalInvoice, String> extractor = CanonicalFieldRegistry.getExtractor(fieldPath);

        if (extractor == null) {
            return null;
        }

        // TODO: Отхэндлить
        try {
            return extractor.apply(invoice);
        } catch (NullPointerException e) {
            return null;
        }
    }
}