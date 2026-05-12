package com.vpvpteam.xmlinvoicevalidationbackend.validation.repository;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.VendorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VendorRepository extends JpaRepository<VendorEntity, Long> {

    Optional<VendorEntity> findByTaxId(String taxId);
}