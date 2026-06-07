package com.vpvpteam.xmlinvoicevalidationbackend.validation.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "validation_batch")
public class ValidationBatchEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false, unique = true, length = 36)
    private String batchId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "batch_vendor",
            joinColumns = @JoinColumn(name = "validation_batch_id")
    )
    @Column(name = "vendor_id", nullable = false, length = 50)
    private List<String> vendorIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "batch_invoice",
            joinColumns = @JoinColumn(name = "validation_batch_id")
    )
    @Column(name = "invoice_id", nullable = false, length = 100)
    private List<String> invoiceIds = new ArrayList<>();

    @OneToOne(mappedBy = "validationBatch", fetch = FetchType.LAZY)
    private ValidationOutputEntity validationOutput;
}