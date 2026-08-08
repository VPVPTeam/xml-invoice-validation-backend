package com.vpvpteam.xmlinvoicevalidationbackend.validation.controller;

import com.vpvpteam.xmlinvoicevalidationbackend.api.exceptions.ApiBadRequestException;
import com.vpvpteam.xmlinvoicevalidationbackend.util.ExceptionUtils;
import com.vpvpteam.xmlinvoicevalidationbackend.util.FieldCheck;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.XmlFileData;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts uploaded multipart files into the format-independent input of the validation service.
 * Belongs to the web layer: MultipartFile is a Spring Web type and must not reach the service.
 * Missing upload is returned as an empty list; rejecting an empty batch is the service's decision.
 */
final class XmlFileDataReader {
    private static final String UNKNOWN_FILE_NAME = "UNKNOWN";

    private XmlFileDataReader() {}

    static List<XmlFileData> read(List<MultipartFile> files) {
        if (files == null) {
            return List.of();
        }

        List<XmlFileData> xmlFilesData = new ArrayList<>(files.size());

        for (MultipartFile file : files) {
            xmlFilesData.add(read(file));
        }

        return xmlFilesData;
    }

    private static XmlFileData read(MultipartFile file) {
        if (file == null) {
            throw new ApiBadRequestException("XML file is null");
        }

        String fileName = FieldCheck.notNullNorBlank(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : UNKNOWN_FILE_NAME;

        if (file.isEmpty()) {
            throw new ApiBadRequestException("Empty XML file: " + fileName);
        }

        try {
            return new XmlFileData(fileName, new ByteArrayInputStream(file.getBytes()));
        } catch (IOException ex) {
            throw new ApiBadRequestException("Cannot read XML file: " + fileName
                    + ": " + ExceptionUtils.safeMessage(ex));
        }
    }
}