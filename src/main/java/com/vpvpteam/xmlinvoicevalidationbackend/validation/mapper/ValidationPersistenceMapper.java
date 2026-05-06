package com.vpvpteam.xmlinvoicevalidationbackend.validation.mapper;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationBatchEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationIssueEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationOutputEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationBatch;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationOutput;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ValidationPersistenceMapper {

    // Model-> Entity
    public ValidationBatchEntity toEntity(ValidationBatch batchModel) {
        ValidationBatchEntity batchEntity = new ValidationBatchEntity();

        batchEntity.setBatchId(batchModel.getBatchId());
        batchEntity.setCreatedAt(batchModel.getCreatedAt());
        batchEntity.setVendorIds(new ArrayList<>(batchModel.getListOfVendorIds()));
        batchEntity.setInvoiceIds(new ArrayList<>(batchModel.getListOfInvoiceIds()));

        return batchEntity;
    }

    public ValidationOutputEntity toEntity(ValidationOutput outputModel, ValidationBatchEntity batchEntity) {
        ValidationOutputEntity outputEntity = new ValidationOutputEntity();

        outputEntity.setValidationBatch(batchEntity);
        outputEntity.setStatus(outputModel.getStatus());
        outputEntity.setTotalInvoices(outputModel.getTotalInvoices());
        outputEntity.setValidInvoices(outputModel.getValidInvoices());
        outputEntity.setInvoicesWithIssues(outputModel.getInvoicesWithIssues());
        outputEntity.setDuplicateInvoices(outputModel.getDuplicateInvoices());

        for (ValidationIssueEntity issueEntity : toEntity(outputModel.getIssues())) {
            outputEntity.addIssue(issueEntity);
        }

        return outputEntity;
    }

    private List<ValidationIssueEntity> toEntity(List<ValidationIssue> issueModels) {
        List<ValidationIssueEntity> result = new ArrayList<>();
        if (issueModels == null) {
            return result;
        }

        for (ValidationIssue issueModel : issueModels) {
            ValidationIssueEntity issueEntity = new ValidationIssueEntity();

            issueEntity.setFileName(issueModel.getFileName());
            issueEntity.setInvoiceNumber(issueModel.getInvoiceNumber());
            issueEntity.setSellerTaxId(issueModel.getSellerTaxId());
            issueEntity.setSeverity(issueModel.getSeverity());
            issueEntity.setStage(issueModel.getStage());
            issueEntity.setRuleKey(issueModel.getRuleKey());
            issueEntity.setFieldPath(issueModel.getFieldPath());
            result.add(issueEntity);
        }

        return result;
    }

    // Entity -> Model
    public ValidationBatch toModel(ValidationBatchEntity batchEntity) {
        ValidationBatch batch = new ValidationBatch(batchEntity.getBatchId(), batchEntity.getCreatedAt());

        batch.setListOfVendorIds(new ArrayList<>(batchEntity.getVendorIds()));
        batch.setListOfInvoiceIds(new ArrayList<>(batchEntity.getInvoiceIds()));

        return batch;
    }

    public ValidationOutput toModel(ValidationOutputEntity outputEntity) {
        ValidationBatch batch = toModel(outputEntity.getValidationBatch());
        ValidationOutput output = new ValidationOutput(batch);

        output.setStatus(outputEntity.getStatus());
        output.setTotalInvoices(outputEntity.getTotalInvoices());
        output.setValidInvoices(outputEntity.getValidInvoices());
        output.setInvoicesWithIssues(outputEntity.getInvoicesWithIssues());
        output.setDuplicateInvoices(outputEntity.getDuplicateInvoices());
        output.setIssues(toModel(outputEntity.getIssues()));

        return output;
    }

    public ValidationIssue toModel(ValidationIssueEntity issueEntity) {
        ValidationIssue issue = new ValidationIssue();

        issue.setFileName(issueEntity.getFileName());
        issue.setInvoiceNumber(issueEntity.getInvoiceNumber());
        issue.setSellerTaxId(issueEntity.getSellerTaxId());
        issue.setSeverity(issueEntity.getSeverity());
        issue.setStage(issueEntity.getStage());
        issue.setRuleKey(issueEntity.getRuleKey());
        issue.setFieldPath(issueEntity.getFieldPath());

        return issue;
    }

    private List<ValidationIssue> toModel(List<ValidationIssueEntity> IssueEntities) {
        if (IssueEntities == null) {
            return new ArrayList<>();
        }

        List<ValidationIssue> result = new ArrayList<>();
        for (ValidationIssueEntity issueEntity : IssueEntities) {
            result.add(toModel(issueEntity));
        }

        return result;
    }
}