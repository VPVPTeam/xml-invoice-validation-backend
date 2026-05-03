package com.vpvpteam.xmlinvoicevalidationbackend.validation.mapper;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationBatchEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationIssueEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationOutputEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationOutput;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ValidationPersistenceMapper {

    public ValidationBatchEntity toBatchEntity(ValidationOutput output) {
        ValidationBatchEntity batchEntity = new ValidationBatchEntity();
        batchEntity.setBatchId(output.getValidationBatch().getBatchId());
        batchEntity.setCreatedAt(output.getValidationBatch().getCreatedAt());
        batchEntity.setVendorIds(new ArrayList<>(output.getValidationBatch().getListOfVendorIds()));
        batchEntity.setInvoiceIds(new ArrayList<>(output.getValidationBatch().getListOfInvoiceIds()));
        return batchEntity;
    }

    public ValidationOutputEntity toOutputEntity(ValidationOutput output, ValidationBatchEntity batchEntity) {
        ValidationOutputEntity outputEntity = new ValidationOutputEntity();
        outputEntity.setValidationBatch(batchEntity);
        outputEntity.setStatus(output.getStatus());
        outputEntity.setTotalInvoices(output.getTotalInvoices());
        outputEntity.setValidInvoices(output.getValidInvoices());
        outputEntity.setInvoicesWithIssues(output.getInvoicesWithIssues());
        outputEntity.setDuplicateInvoices(output.getDuplicateInvoices());

        for (ValidationIssueEntity issueEntity : toIssueEntities(output.getIssues())) {
            outputEntity.addIssue(issueEntity);
        }

        return outputEntity;
    }

    private List<ValidationIssueEntity> toIssueEntities(List<ValidationIssue> issues) {
        List<ValidationIssueEntity> result = new ArrayList<>();
        if (issues == null) {
            return result;
        }

        for (ValidationIssue issue : issues) {
            ValidationIssueEntity issueEntity = new ValidationIssueEntity();
            issueEntity.setFileName(issue.getFileName());
            issueEntity.setInvoiceNumber(issue.getInvoiceNumber());
            issueEntity.setSellerTaxId(issue.getSellerTaxId());
            issueEntity.setSeverity(issue.getSeverity());
            issueEntity.setStage(issue.getStage());
            issueEntity.setRuleKey(issue.getRuleKey());
            issueEntity.setFieldPath(issue.getFieldPath());
            result.add(issueEntity);
        }

        return result;
    }
}