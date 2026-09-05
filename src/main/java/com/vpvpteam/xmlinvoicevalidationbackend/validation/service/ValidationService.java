package com.vpvpteam.xmlinvoicevalidationbackend.validation.service;

import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.EmptyBatchException;
import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.UnsupportedFormatException;
import com.vpvpteam.xmlinvoicevalidationbackend.formats.FormatProcessor;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.dao.ValidationBatchDao;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.BusinessRuleEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.message.DuplicateIssueMessages;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.message.RuleKeys;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.message.TechnicalIssueMessages;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.InvoiceId;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationOutput;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.XmlFileData;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.BusinessRuleRepository;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.business.BusinessValidationOutput;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.business.BusinessValidator;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.technical.TechnicalValidationOutput;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service that validates a batch of XML invoice/invoices.
 * Files are parsed and technically validated in parallel; the database is queried
 * in bulk; the final result is assembled sequentially.
 */
@Service
public final class ValidationService {
    private final Map<String, FormatProcessor> processors;
    private final BusinessValidator businessValidator;
    private final ValidationPersistenceService persistenceService;
    private final ValidationBatchDao validationBatchDao;
    private final BusinessRuleRepository businessRuleRepository;

    public ValidationService(List<FormatProcessor> processorList,
                             BusinessValidator businessValidator,
                             ValidationPersistenceService persistenceService,
                             ValidationBatchDao validationBatchDao,
                             BusinessRuleRepository businessRuleRepository) {
        this.processors = processorList.stream()
                .collect(Collectors.toMap(FormatProcessor::getFormatName, Function.identity()));
        this.businessValidator = businessValidator;
        this.persistenceService = persistenceService;
        this.validationBatchDao = validationBatchDao;
        this.businessRuleRepository = businessRuleRepository;
    }

    public ValidationOutput validateBatch(List<XmlFileData> xmlFilesData, String format) {
        ensureBatchIsNotEmpty(xmlFilesData);
        FormatProcessor processor = resolveProcessor(format);

        // Phase 1: parse + technical validation of every file IN PARALLEL (pure CPU, no DB, no shared state).
        List<TechnicalResult> technicalResults = xmlFilesData.parallelStream()
                .map(xmlFileData -> parseAndValidateTechnically(xmlFileData, processor))
                .toList();

        // Phase 2: load everything the batch needs from the DB in TWO bulk queries.
        Map<String, Map<String, OffsetDateTime>> crossBatchDuplicates = validationBatchDao.findBatchesByInvoiceIds(parsedInvoiceIds(technicalResults));
        Map<String, List<BusinessRuleEntity>> rulesByVendorTaxId = loadRules(vendorTaxIdsNeedingRules(technicalResults));

        // Phase 3: assemble the result SEQUENTIALLY (single thread → accumulator stays simple).
        BatchAccumulator accumulator = new BatchAccumulator();
        for (TechnicalResult result : technicalResults) {
            accumulate(result, crossBatchDuplicates, rulesByVendorTaxId, accumulator);
        }

        ValidationOutput output = accumulator.toOutput();
        persistenceService.save(output);

        return output;
    }

    private void ensureBatchIsNotEmpty(List<XmlFileData> xmlFilesData) {
        if (xmlFilesData == null || xmlFilesData.isEmpty()) {
            throw new EmptyBatchException("Batch has no XML files to validate");
        }
    }

    private FormatProcessor resolveProcessor(String format) {
        FormatProcessor processor = processors.get(format);

        if (processor == null) {
            throw new UnsupportedFormatException("Unsupported format: " + format
                    + ". Supported: " + processors.keySet());
        }

        return processor;
    }

    private TechnicalResult parseAndValidateTechnically(XmlFileData xmlFileData, FormatProcessor processor) {
        try {
            TechnicalValidationOutput output = processor.process(xmlFileData.xmlInputStream(), xmlFileData.fileName());

            return new TechnicalResult(xmlFileData.fileName(), output.getInvoiceId(), output, null);
        } catch (Exception ex) {
            ValidationIssue parseError = ValidationIssue.technicalError(
                    xmlFileData.fileName(),
                    InvoiceId.NONE,
                    RuleKeys.TECH_XML_PARSE_ERROR,
                    "xml",
                    TechnicalIssueMessages.xmlParseError(xmlFileData.fileName(), ex)
            );

            return new TechnicalResult(xmlFileData.fileName(), InvoiceId.NONE, null, parseError);
        }
    }

    private static Set<String> parsedInvoiceIds(List<TechnicalResult> results) {
        return results.stream()
                .filter(result -> !result.parseFailed())
                .map(result -> result.invoiceId().value())
                .collect(Collectors.toSet());
    }

    private static Set<String> vendorTaxIdsNeedingRules(List<TechnicalResult> results) {
        return results.stream()
                .filter(result -> !result.parseFailed() && !result.output().hasErrors())
                .map(result -> result.invoiceId().sellerTaxId())
                .collect(Collectors.toSet());
    }

    private Map<String, List<BusinessRuleEntity>> loadRules(Set<String> vendorTaxIds) {
        if (vendorTaxIds.isEmpty()) {
            return Map.of();
        }

        return businessRuleRepository.findAllByVendorTaxIdIn(vendorTaxIds).stream()
                .collect(Collectors.groupingBy(rule -> rule.getVendor().getTaxId()));
    }

    private void accumulate(TechnicalResult result,
                            Map<String, Map<String, OffsetDateTime>> crossBatchDuplicates,
                            Map<String, List<BusinessRuleEntity>> rulesByVendorTaxId,
                            BatchAccumulator accumulator) {
        if (result.parseFailed()) {
            accumulator.addIssue(result.parseError());
            return;
        }

        InvoiceId invoiceId = result.invoiceId();

        if (!accumulator.registerInvoice(invoiceId)) {
            accumulator.addDuplicateIssue(ValidationIssue.businessWarning(
                    result.fileName(),
                    invoiceId,
                    RuleKeys.DUPLICATE_IN_BATCH,
                    "invoiceId",
                    DuplicateIssueMessages.duplicateInBatch()
            ));
            return;
        }

        Map<String, OffsetDateTime> previousBatches = crossBatchDuplicates.getOrDefault(invoiceId.value(), Map.of());

        if (!previousBatches.isEmpty()) {
            accumulator.addDuplicateIssue(ValidationIssue.businessWarning(
                    result.fileName(),
                    invoiceId,
                    RuleKeys.DUPLICATE_CROSS_BATCH,
                    "invoiceId",
                    DuplicateIssueMessages.duplicateCrossBatch(previousBatches)
            ));
        }

        TechnicalValidationOutput technicalOutput = result.output();
        if (technicalOutput.hasErrors()) {
            accumulator.addIssues(technicalOutput.getIssues());
            return;
        }

        List<BusinessRuleEntity> rules = rulesByVendorTaxId.getOrDefault(invoiceId.sellerTaxId(), List.of());
        BusinessValidationOutput businessOutput = businessValidator.validate(technicalOutput.getCanonicalInvoice(), result.fileName(), rules);
        accumulator.addIssues(businessOutput.getIssues());
    }

    private record TechnicalResult(String fileName,
                                   InvoiceId invoiceId,
                                   TechnicalValidationOutput output,
                                   ValidationIssue parseError) {
        boolean parseFailed() {
            return parseError != null;
        }
    }
}