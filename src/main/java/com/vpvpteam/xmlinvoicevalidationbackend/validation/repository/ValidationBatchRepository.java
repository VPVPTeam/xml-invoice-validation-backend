package com.vpvpteam.xmlinvoicevalidationbackend.validation.repository;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ValidationBatchRepository extends JpaRepository<ValidationBatchEntity, Long> {
    @Query("SELECT batch FROM ValidationBatchEntity batch " +
            "JOIN batch.vendorIds vendor " +
            "WHERE vendor = :vendorId " +
            "ORDER BY batch.createdAt DESC")
    List<ValidationBatchEntity> findBatchesByVendorId(@Param("vendorId") String vendorId);
}