package com.vpvpteam.xmlinvoicevalidationbackend.validation.service;

import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.EntityAlreadyExistsException;
import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.EntityNotFoundException;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.VendorEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.mapper.PersistenceMapper;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.Vendor;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.VendorRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write operations on vendors.
 */
@Service
@AllArgsConstructor
public class VendorPersistenceService {
    private final PersistenceMapper mapper;
    private final VendorRepository vendorRepository;

    @Transactional
    public Vendor save(Vendor vendor) {
        checkVendorNotExistsOrThrow(vendor.getTaxId());
        VendorEntity saved = vendorRepository.save(mapper.toEntity(vendor));

        return mapper.toModel(saved);
    }

    @Transactional
    public Vendor update(Vendor vendor) {
        VendorEntity vendorEntity = getVendorEntityOrThrow(vendor.getTaxId());
        vendorEntity.setName(vendor.getName());

        return mapper.toModel(vendorEntity);
    }

    @Transactional
    public void delete(String taxId) {
        VendorEntity vendorEntity = getVendorEntityOrThrow(taxId);
        vendorRepository.delete(vendorEntity);
    }

    private VendorEntity getVendorEntityOrThrow(String taxId) {
        return vendorRepository.findByTaxId(taxId)
                .orElseThrow(() -> new EntityNotFoundException("Vendor not found: " + taxId));
    }

    private void checkVendorNotExistsOrThrow(String vendorTaxId) {
        if (vendorRepository.findByTaxId(vendorTaxId).isPresent()) {
            throw new EntityAlreadyExistsException("Vendor already exists: " + vendorTaxId);
        }
    }
}