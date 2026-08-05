package com.vpvpteam.xmlinvoicevalidationbackend.validation.controller;

import com.vpvpteam.xmlinvoicevalidationbackend.api.ListResponse;
import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.EntityNotFoundException;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationOutputEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.mapper.PersistenceMapper;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.*;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.service.ValidationQueryService;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.service.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/validation")
public final class InvoiceValidationController {
    private final ValidationService validationService;
    private final ValidationQueryService queryService;
    private final PersistenceMapper persistenceMapper;

    @PostMapping(
            value = "/validate",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ValidationOutput validateXmlInputs(@RequestParam("file") List<MultipartFile> files,
                                              @RequestParam("format") String format) {
        return validationService.validateBatch(XmlFileDataReader.read(files), format);
    }

    @GetMapping("/report/{batchId}")
    public ValidationOutput getReport(@PathVariable String batchId) {
        ValidationOutputEntity outputEntity = queryService.getFullReport(batchId)
                .orElseThrow(() -> new EntityNotFoundException("Batch not found: " + batchId));

        return persistenceMapper.toModel(outputEntity);
    }

    @GetMapping("/batches/by-vendor")
    public ListResponse<BatchSummary> getBatchesByVendor(@RequestParam String vendorId) {
        List<BatchSummary> batches = queryService.getBatchesByVendor(vendorId)
                .stream()
                .map(entity -> new BatchSummary(entity.getBatchId(), entity.getCreatedAt()))
                .toList();

        return ListResponse.of(batches);
    }

    @GetMapping("/issues/by-invoice")
    public ListResponse<ValidationIssue> getIssuesByInvoice(@RequestParam String sellerTaxId,
                                                            @RequestParam String invoiceNumber) {
        List<ValidationIssue> issues = queryService.getIssuesByInvoice(sellerTaxId, invoiceNumber)
                .stream()
                .map(persistenceMapper::toModel)
                .toList();

        return ListResponse.of(issues);
    }

    @GetMapping("/batches/by-invoice")
    public ListResponse<BatchSummary> getBatchesByInvoice(@RequestParam String sellerTaxId,
                                                          @RequestParam String invoiceNumber) {
        String invoiceId = new InvoiceId(sellerTaxId, invoiceNumber).value();

        List<BatchSummary> batches = queryService.getBatchesByInvoiceId(invoiceId)
                .entrySet().stream()
                .map(entry -> new BatchSummary(entry.getKey(), entry.getValue()))
                .toList();

        return ListResponse.of(batches);
    }
}