package com.vpvpteam.xmlinvoicevalidationbackend.validation.repository;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.BatchFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BatchFileRepository extends JpaRepository<BatchFileEntity, Long> {
    List<BatchFileEntity> findAllByValidationBatch_BatchIdOrderByIdAsc(String batchId);
}