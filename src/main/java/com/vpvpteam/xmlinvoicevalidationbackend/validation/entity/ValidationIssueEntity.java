package com.vpvpteam.xmlinvoicevalidationbackend.validation.entity;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.ValidationStage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "validation_issue")
public class ValidationIssueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "validation_output_id", nullable = false)
    private ValidationOutputEntity validationOutput;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    @Column(name = "seller_tax_id", length = 50)
    private String sellerTaxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 20)
    private ValidationStage stage;

    @Column(name = "rule_key", nullable = false, length = 100)
    private String ruleKey;

    @Column(name = "field_path", length = 100)
    private String fieldPath;
}