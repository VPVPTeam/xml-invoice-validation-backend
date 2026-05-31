package com.vpvpteam.xmlinvoicevalidationbackend.validation.entity;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.RuleOperator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "business_rule")
public final class BusinessRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private VendorEntity vendor;

    @Column(name = "rule_key", nullable = false, length = 100)
    private String ruleKey;

    @Column(name = "field_path", nullable = false, length = 100)
    private String fieldPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false, length = 30)
    private RuleOperator operator;

    @Column(name = "expected_value", nullable = false, length = 255)
    private String expectedValue;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy = "system";
}