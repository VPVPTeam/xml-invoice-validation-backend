package com.vpvpteam.xmlinvoicevalidationbackend.validation.mapper;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.*;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ValidationPersistenceMapper {

    // Batch
    public ValidationBatch toModel(ValidationBatchEntity batchEntity) {
        ValidationBatch batch = new ValidationBatch(batchEntity.getBatchId(), batchEntity.getCreatedAt());

        batch.setListOfVendorIds(new ArrayList<>(batchEntity.getVendorIds()));
        batch.setListOfInvoiceIds(new ArrayList<>(batchEntity.getInvoiceIds()));

        return batch;
    }

    public ValidationBatchEntity toEntity(ValidationBatch batchModel) {
        ValidationBatchEntity batchEntity = new ValidationBatchEntity();

        batchEntity.setBatchId(batchModel.getBatchId());
        batchEntity.setCreatedAt(batchModel.getCreatedAt());
        batchEntity.setVendorIds(new ArrayList<>(batchModel.getListOfVendorIds()));
        batchEntity.setInvoiceIds(new ArrayList<>(batchModel.getListOfInvoiceIds()));

        return batchEntity;
    }

    // Output

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

    // Issue

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


    // Vendor

    public Vendor toModel(VendorEntity entity) {
        Vendor vendor = new Vendor();

        vendor.setId(entity.getId());
        vendor.setName(entity.getTaxId());
        vendor.setName(entity.getName());

        return vendor;
    }

    public VendorEntity toEntity(Vendor model) {
        VendorEntity entity = new VendorEntity();

        entity.setTaxId(model.getTaxId());
        entity.setName(model.getName());

        return entity;
    }

    // BusinessRule
    public BusinessRule toModel(BusinessRuleEntity entity) {
        BusinessRule rule = new BusinessRule();

        rule.setId(entity.getId());
        rule.setVendorTaxId(entity.getVendor().getTaxId());
        rule.setRuleKey(entity.getRuleKey());
        rule.setFieldPath(entity.getFieldPath());
        rule.setOperator(entity.getOperator());
        rule.setExpectedValue(entity.getExpectedValue());
        rule.setCreatedBy(entity.getCreatedBy());

        return rule;
    }
    public BusinessRuleEntity toEntity(BusinessRule model, VendorEntity vendorEntity) {
        BusinessRuleEntity entity = new BusinessRuleEntity();

        entity.setVendor(vendorEntity);
        entity.setRuleKey(model.getRuleKey());
        entity.setFieldPath(model.getFieldPath());
        entity.setOperator(model.getOperator());
        entity.setExpectedValue(model.getExpectedValue());
        entity.setCreatedBy(model.getCreatedBy());

        return entity;
    }
}