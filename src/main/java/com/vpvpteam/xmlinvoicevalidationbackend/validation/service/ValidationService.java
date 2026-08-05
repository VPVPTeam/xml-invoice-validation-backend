package com.vpvpteam.xmlinvoicevalidationbackend.validation.service;

import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.UnsupportedFormatException;
import com.vpvpteam.xmlinvoicevalidationbackend.formats.FormatProcessor;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.dao.ValidationBatchDao;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.message.DuplicateIssueMessages;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.message.RuleKeys;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.message.TechnicalIssueMessages;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.InvoiceId;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationBatch;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationOutput;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.XmlFileData;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.business.BusinessValidationOutput;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.business.BusinessValidator;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.technical.TechnicalValidationOutput;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service that validates a batch of XML invoice/invoices.
 * Orchestrates parsing, duplicate checks, technical validation and business validation.
 */
@Service
public final class ValidationService {
    private final Map<String, FormatProcessor> processors;
    private final BusinessValidator businessValidator;
    private final ValidationPersistenceService persistenceService;
    private final ValidationBatchDao validationBatchDao;

    public ValidationService(List<FormatProcessor> processorList,
                             BusinessValidator businessValidator,
                             ValidationPersistenceService persistenceService,
                             ValidationBatchDao validationBatchDao) {
        this.processors = processorList.stream()
                .collect(Collectors.toMap(FormatProcessor::getFormatName, Function.identity()));
        this.businessValidator = businessValidator;
        this.persistenceService = persistenceService;
        this.validationBatchDao = validationBatchDao;
    }

    public ValidationOutput validateBatch(List<XmlFileData> xmlFilesData, String format) {
        ValidationBatch batch = new ValidationBatch();

        if (xmlFilesData == null || xmlFilesData.isEmpty()) {
            return emptyBatchOutput(batch);
        }

        FormatProcessor processor = resolveProcessor(format);
        BatchAccumulator accumulator = new BatchAccumulator();

        for (XmlFileData xmlFileData : xmlFilesData) {
            validateFile(xmlFileData, processor, accumulator);
        }

        ValidationOutput output = accumulator.toOutput(batch);
        persistenceService.save(output);

        return output;
    }

    private FormatProcessor resolveProcessor(String format) {
        FormatProcessor processor = processors.get(format);

        if (processor == null) {
            throw new UnsupportedFormatException("Unsupported format: " + format
                    + ". Supported: " + processors.keySet());
        }

        return processor;
    }

    private ValidationOutput emptyBatchOutput(ValidationBatch batch) {
        BatchAccumulator accumulator = new BatchAccumulator();

        accumulator.addIssue(ValidationIssue.technicalError(
                "",
                InvoiceId.NONE,
                RuleKeys.TECH_EMPTY_BATCH,
                "batch",
                TechnicalIssueMessages.emptyBatch()
        ));

        return accumulator.toOutput(batch);
    }

    private void validateFile(XmlFileData xmlFileData, FormatProcessor processor, BatchAccumulator accumulator) {
        TechnicalValidationOutput technicalOutput;
        try {
            technicalOutput = processor.process(xmlFileData.xmlInputStream(), xmlFileData.fileName());
        } catch (Exception ex) {
            accumulator.addIssue(ValidationIssue.technicalError(
                    xmlFileData.fileName(),
                    InvoiceId.NONE,
                    RuleKeys.TECH_XML_PARSE_ERROR,
                    "xml",
                    TechnicalIssueMessages.xmlParseError(xmlFileData.fileName(), ex)
            ));
            return;
        }

        InvoiceId invoiceId = new InvoiceId(technicalOutput.getSellerTaxId(), technicalOutput.getInvoiceNumber());

        if (!accumulator.registerInvoice(invoiceId)) {
            accumulator.addDuplicateIssue(ValidationIssue.businessWarning(
                    xmlFileData.fileName(),
                    invoiceId,
                    RuleKeys.DUPLICATE_IN_BATCH,
                    "invoiceId",
                    DuplicateIssueMessages.duplicateInBatch()
            ));
            return;
        }

        checkPreviousBatches(xmlFileData.fileName(), invoiceId, accumulator);

        if (technicalOutput.hasErrors()) {
            accumulator.addIssues(technicalOutput.getIssues());
            return;
        }

        BusinessValidationOutput businessOutput = businessValidator.validate(
                technicalOutput.getCanonicalInvoice(),
                xmlFileData.fileName()
        );
        accumulator.addIssues(businessOutput.getIssues());
    }

    private void checkPreviousBatches(String fileName, InvoiceId invoiceId, BatchAccumulator accumulator) {
        Map<String, OffsetDateTime> previousBatches = validationBatchDao.findBatchesByInvoiceId(invoiceId.value());

        if (previousBatches.isEmpty()) {
            return;
        }

        accumulator.addDuplicateIssue(ValidationIssue.businessWarning(
                fileName,
                invoiceId,
                RuleKeys.DUPLICATE_CROSS_BATCH,
                "invoiceId",
                DuplicateIssueMessages.duplicateCrossBatch(previousBatches)
        ));
    }
}