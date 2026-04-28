package com.vpvpteam.xmlinvoicevalidationbackend.validation.entity;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "validation_output")
public class ValidationOutputEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "validation_batch_id", nullable = false, unique = true)
    private ValidationBatchEntity validationBatch;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Severity status = Severity.OK;

    @Column(name = "total_invoices", nullable = false)
    private int totalInvoices = 0;

    @Column(name = "valid_invoices", nullable = false)
    private int validInvoices = 0;

    @Column(name = "invoices_with_issues", nullable = false)
    private int invoicesWithIssues = 0;

    @Column(name = "duplicate_invoices", nullable = false)
    private int duplicateInvoices = 0;

    @OneToMany(
            mappedBy = "validationOutput",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ValidationIssueEntity> issues = new ArrayList<>();

    // FIXME: Посмотреть где используется и нужно ли это
//    public void addIssue(ValidationIssueEntity issue) {
//        if (issue == null) {
//            return;
//        }
//        issues.add(issue);
//        issue.setValidationOutput(this);
//    }
//
//    public void removeIssue(ValidationIssueEntity issue) {
//        if (issue == null) {
//            return;
//        }
//        issues.remove(issue);
//        issue.setValidationOutput(null);
//    }
}