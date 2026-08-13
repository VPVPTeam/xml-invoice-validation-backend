package com.vpvpteam.xmlinvoicevalidationbackend.validation.mapper;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationBatchEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationIssueEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationOutputEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.VendorEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.ValidationStage;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.message.RuleKeys;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.InvoiceId;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationBatch;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationOutput;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.Vendor;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceMapperTest {

    private static final String BATCH_ID = "3f2504e0-4f89-11d3-9a0c-0305e82c3301";
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2025-03-14T10:15:30Z");
    private static final InvoiceId GOODYEAR = new InvoiceId("5211146938", "5860135336");

    private final PersistenceMapper mapper = new PersistenceMapper();

    // Batch

    @Test
    void toEntity_batch_copiesAllFields() {
        ValidationBatch batch = ValidationBatch.restored(
                BATCH_ID, CREATED_AT, List.of("5211146938"), List.of(GOODYEAR.value()));

        ValidationBatchEntity entity = mapper.toEntity(batch);

        assertThat(entity.getBatchId()).isEqualTo(BATCH_ID);
        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(entity.getVendorIds()).containsExactly("5211146938");
        assertThat(entity.getInvoiceIds()).containsExactly(GOODYEAR.value());
    }

    @Test
    void toModel_batch_copiesAllFields() {
        ValidationBatchEntity entity = batchEntity();

        ValidationBatch batch = mapper.toModel(entity);

        assertThat(batch.getBatchId()).isEqualTo(BATCH_ID);
        assertThat(batch.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(batch.getListOfVendorIds()).containsExactly("5211146938");
        assertThat(batch.getListOfInvoiceIds()).containsExactly(GOODYEAR.value());
    }

    // Output

    @Test
    void toEntity_output_copiesCountersStatusAndIssues() {
        ValidationBatch batch = ValidationBatch.restored(
                BATCH_ID, CREATED_AT, List.of("5211146938"), List.of(GOODYEAR.value()));
        ValidationOutput output = ValidationOutput.of(batch, List.of(technicalIssue()), 0);

        ValidationOutputEntity entity = mapper.toEntity(output, batchEntity());

        assertThat(entity.getStatus()).isEqualTo(Severity.ERROR);
        assertThat(entity.getTotalInvoices()).isEqualTo(1);
        assertThat(entity.getValidInvoices()).isZero();
        assertThat(entity.getInvoicesWithIssues()).isEqualTo(1);
        assertThat(entity.getDuplicateInvoices()).isZero();
        assertThat(entity.getIssues()).hasSize(1);
    }

    @Test
    void toEntity_output_linksIssuesBackToOutput() {
        ValidationBatch batch = ValidationBatch.restored(
                BATCH_ID, CREATED_AT, List.of("5211146938"), List.of(GOODYEAR.value()));
        ValidationOutput output = ValidationOutput.of(batch, List.of(technicalIssue()), 0);

        ValidationOutputEntity entity = mapper.toEntity(output, batchEntity());

        assertThat(entity.getIssues().get(0).getValidationOutput()).isSameAs(entity);
    }

    @Test
    void toModel_output_restoresCountersAsStored() {
        ValidationOutputEntity entity = outputEntity(Severity.WARNING, 5, 4, 1, 2);

        ValidationOutput output = mapper.toModel(entity);

        assertThat(output.getStatus()).isEqualTo(Severity.WARNING);
        assertThat(output.getTotalInvoices()).isEqualTo(5);
        assertThat(output.getValidInvoices()).isEqualTo(4);
        assertThat(output.getInvoicesWithIssues()).isEqualTo(1);
        assertThat(output.getDuplicateInvoices()).isEqualTo(2);
        assertThat(output.getValidationBatch().getBatchId()).isEqualTo(BATCH_ID);
    }

    @Test
    void toModel_output_withoutIssues_returnsEmptyList() {
        ValidationOutputEntity entity = outputEntity(Severity.OK, 1, 1, 0, 0);
        entity.setIssues(null);

        ValidationOutput output = mapper.toModel(entity);

        assertThat(output.getIssues()).isEmpty();
    }

    // Issue

    @Test
    void toEntity_issue_copiesAllPersistedFields() {
        ValidationOutputEntity entity = mapper.toEntity(
                ValidationOutput.of(
                        ValidationBatch.restored(BATCH_ID, CREATED_AT, List.of(), List.of(GOODYEAR.value())),
                        List.of(technicalIssue()),
                        0),
                batchEntity());

        ValidationIssueEntity issueEntity = entity.getIssues().get(0);

        assertThat(issueEntity.getFileName()).isEqualTo("Goodyear.xml");
        assertThat(issueEntity.getSellerTaxId()).isEqualTo(GOODYEAR.sellerTaxId());
        assertThat(issueEntity.getInvoiceNumber()).isEqualTo(GOODYEAR.invoiceNumber());
        assertThat(issueEntity.getStage()).isEqualTo(ValidationStage.TECHNICAL);
        assertThat(issueEntity.getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(issueEntity.getRuleKey()).isEqualTo(RuleKeys.TECH_MISSING_REQUIRED_FIELD);
        assertThat(issueEntity.getFieldPath()).isEqualTo("Fa.P_2");
    }

    @Test
    void toModel_issue_copiesAllPersistedFields() {
        ValidationIssueEntity entity = issueEntity();

        ValidationIssue issue = mapper.toModel(entity);

        assertThat(issue.getFileName()).isEqualTo("Goodyear.xml");
        assertThat(issue.getSellerTaxId()).isEqualTo(GOODYEAR.sellerTaxId());
        assertThat(issue.getInvoiceNumber()).isEqualTo(GOODYEAR.invoiceNumber());
        assertThat(issue.getStage()).isEqualTo(ValidationStage.TECHNICAL);
        assertThat(issue.getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(issue.getRuleKey()).isEqualTo(RuleKeys.TECH_MISSING_REQUIRED_FIELD);
        assertThat(issue.getFieldPath()).isEqualTo("Fa.P_2");
    }

    @Test
    void toModel_issue_losesMessageBecauseItIsNotPersisted() {
        ValidationIssue issue = mapper.toModel(issueEntity());

        assertThat(issue.getMessage()).isNull();
    }

    // Vendor

    @Test
    void toModel_vendor_copiesAllFields() {
        VendorEntity entity = new VendorEntity();
        entity.setTaxId("5211146938");
        entity.setName("Goodyear Polska Sp. z o.o.");

        Vendor vendor = mapper.toModel(entity);

        assertThat(vendor.getTaxId()).isEqualTo("5211146938");
        assertThat(vendor.getName()).isEqualTo("Goodyear Polska Sp. z o.o.");
    }

    @Test
    void toEntity_vendor_doesNotCopyId() {
        Vendor vendor = new Vendor();
        vendor.setId(42L);
        vendor.setTaxId("5211146938");
        vendor.setName("Goodyear Polska Sp. z o.o.");

        VendorEntity entity = mapper.toEntity(vendor);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getTaxId()).isEqualTo("5211146938");
        assertThat(entity.getName()).isEqualTo("Goodyear Polska Sp. z o.o.");
    }

    private static ValidationIssue technicalIssue() {
        return ValidationIssue.technicalError(
                "Goodyear.xml",
                GOODYEAR,
                RuleKeys.TECH_MISSING_REQUIRED_FIELD,
                "Fa.P_2",
                "Required field 'Fa.P_2' is missing or invalid."
        );
    }

    private static ValidationIssueEntity issueEntity() {
        ValidationIssueEntity entity = new ValidationIssueEntity();

        entity.setFileName("Goodyear.xml");
        entity.setSellerTaxId(GOODYEAR.sellerTaxId());
        entity.setInvoiceNumber(GOODYEAR.invoiceNumber());
        entity.setStage(ValidationStage.TECHNICAL);
        entity.setSeverity(Severity.ERROR);
        entity.setRuleKey(RuleKeys.TECH_MISSING_REQUIRED_FIELD);
        entity.setFieldPath("Fa.P_2");

        return entity;
    }

    private static ValidationBatchEntity batchEntity() {
        ValidationBatchEntity entity = new ValidationBatchEntity();

        entity.setBatchId(BATCH_ID);
        entity.setCreatedAt(CREATED_AT);
        entity.setVendorIds(List.of("5211146938"));
        entity.setInvoiceIds(List.of(GOODYEAR.value()));

        return entity;
    }

    private static ValidationOutputEntity outputEntity(Severity status,
                                                       int totalInvoices,
                                                       int validInvoices,
                                                       int invoicesWithIssues,
                                                       int duplicateInvoices) {
        ValidationOutputEntity entity = new ValidationOutputEntity();

        entity.setValidationBatch(batchEntity());
        entity.setStatus(status);
        entity.setTotalInvoices(totalInvoices);
        entity.setValidInvoices(validInvoices);
        entity.setInvoicesWithIssues(invoicesWithIssues);
        entity.setDuplicateInvoices(duplicateInvoices);

        return entity;
    }
}