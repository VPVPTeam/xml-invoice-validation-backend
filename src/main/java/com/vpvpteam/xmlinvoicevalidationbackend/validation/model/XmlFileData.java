package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import java.io.InputStream;

public record XmlFileData(String fileName, InputStream xmlInputStream) {
}