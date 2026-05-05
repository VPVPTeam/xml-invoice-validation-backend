package com.vpvpteam.xmlinvoicevalidationbackend.validation.controller;

import com.vpvpteam.xmlinvoicevalidationbackend.api.exceptions.ApiBadRequestException;
import com.vpvpteam.xmlinvoicevalidationbackend.util.ExceptionUtils;
import com.vpvpteam.xmlinvoicevalidationbackend.util.FieldCheck;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationBatchEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationIssueEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationOutputEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationOutput;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.XmlFileData;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.service.ValidationQueryService;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.service.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/invoices")
public final class InvoiceValidationController {

    private final ValidationService validationService;
    private final ValidationQueryService queryService;

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

            return validationService.validateBatch(xmlFiles);
        } catch (ApiBadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiBadRequestException("XML file is invalid: " + ExceptionUtils.safeMessage(ex));
        }
    }

    @GetMapping("/report/{batchId}")
    public ValidationOutputEntity getReport(@PathVariable String batchId) {
        return queryService.getFullReport(batchId)
                .orElseThrow(() -> new ApiBadRequestException("Batch not found: " + batchId));
    }

    @GetMapping("/batches/by-vendor")
    public List<ValidationBatchEntity> getBatchesByVendor(@RequestParam String vendorId) {
        return queryService.getBatchesByVendor(vendorId);
    }

    @GetMapping("/issues/by-invoice")
    public List<ValidationIssueEntity> getIssuesByInvoice(
            @RequestParam String sellerTaxId,
            @RequestParam String invoiceNumber) {
        return queryService.getIssuesByInvoice(sellerTaxId, invoiceNumber);
    }
}