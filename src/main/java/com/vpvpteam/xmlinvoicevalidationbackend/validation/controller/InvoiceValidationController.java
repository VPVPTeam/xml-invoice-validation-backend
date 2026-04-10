package com.vpvpteam.xmlinvoicevalidationbackend.validation.controller;

import com.vpvpteam.xmlinvoicevalidationbackend.api.exceptions.ApiBadRequestException;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationOutput;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.service.ValidationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            throw new ApiBadRequestException("Pusty batch");
        }

        try {
            List<byte[]> xmlFilesAsBytes = new ArrayList<>(files.size());
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    throw new ApiBadRequestException("Niepoprawny plik XML: " + file.getOriginalFilename());
                }

                xmlFilesAsBytes.add(file.getBytes());
            }

            return validationService.validateBytes(xmlFilesAsBytes);
        } catch (ApiBadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiBadRequestException("Niepoprawny plik XML");
        }
    }
}