package com.vpvpteam.xmlinvoicevalidationbackend.validation.controller;

import com.vpvpteam.xmlinvoicevalidationbackend.api.exceptions.ApiBadRequestException;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationOutput;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.service.ValidationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

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
    public ValidationOutput validateXmlInputs(@RequestParam("file") MultipartFile inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw new ApiBadRequestException("Niepoprawny plik XML");
        }

        inputs.getOriginalFilename();

        try {
            return validateBytes(inputs.getBytes());
        } catch (ApiBadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiBadRequestException("Niepoprawny plik XML");
        }
    }

    private ValidationOutput validateBytes(byte[] xmlBytes) {
        ValidationOutput result = validationService.validateBatch(
                List.of(new ByteArrayInputStream(xmlBytes)),
                UUID.randomUUID().toString()
        );

        // Service parses XML itself. Convert known parse case to HTTP 400.
        boolean hasParseError = result.getIssues() != null && result.getIssues().stream()
                .anyMatch(i -> "TECH_XML_PARSE_ERROR".equals(i.getRuleKey()));

        if (hasParseError) {
            throw new ApiBadRequestException("Niepoprawny plik XML");
        }

        return result;
    }
}