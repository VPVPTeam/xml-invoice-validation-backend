package com.vpvpteam.xmlinvoicevalidationbackend.exceptions;

import com.vpvpteam.xmlinvoicevalidationbackend.canonical.CanonicalFieldRegistry;

import java.util.stream.Collectors;

public final class UnsupportedFieldPathException extends RuntimeException {
    public UnsupportedFieldPathException(String fieldPath) {
        super(buildMessage(fieldPath));
    }

    private static String buildMessage(String fieldPath) {
        String supported = CanonicalFieldRegistry.getSupportedFieldPaths().stream()
                .sorted()
                .collect(Collectors.joining("\n"));
        return "Unsupported field path: '" + fieldPath + "'.\n\nAvailable field paths:\n" + supported;
    }
}