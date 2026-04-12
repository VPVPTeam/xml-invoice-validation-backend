package com.vpvpteam.xmlinvoicevalidationbackend.validation.controller;

import com.vpvpteam.xmlinvoicevalidationbackend.api.exceptions.ApiBadRequestException;
import com.vpvpteam.xmlinvoicevalidationbackend.util.ExceptionUtils;
import com.vpvpteam.xmlinvoicevalidationbackend.util.FieldCheck;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationOutput;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.XmlFileData;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.service.ValidationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceValidationController {

    private final ValidationService validationService;

    public InvoiceValidationController(ValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping(
            value = "/validate",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ValidationOutput validateXmlInputs(@RequestParam("file") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ApiBadRequestException("Empty batch");
        }

        try {
            List<XmlFileData> xmlFiles = new ArrayList<>(files.size());


            for (MultipartFile file : files) {
                if (file == null) {
                    throw new ApiBadRequestException("XML file is null");
                }

                String fileName = (FieldCheck.notNullNorBlank(file.getOriginalFilename()))
                        ? file.getOriginalFilename()
                        : "UNKNOWN";

                if (file.isEmpty()) {
                    throw new ApiBadRequestException("Empty XML file: " + fileName);
                }

                xmlFiles.add(new XmlFileData(fileName, new ByteArrayInputStream(file.getBytes())));
            }

            return validationService.validateFiles(xmlFiles);
        } catch (ApiBadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiBadRequestException("XML file is invalid: " + ExceptionUtils.safeMessage(ex));
        }
    }
}