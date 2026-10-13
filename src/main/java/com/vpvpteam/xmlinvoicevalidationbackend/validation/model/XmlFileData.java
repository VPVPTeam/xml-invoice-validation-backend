package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public record XmlFileData(String fileName, byte[] content) {
    public InputStream xmlInputStream() {
        return new ByteArrayInputStream(content);
    }
}