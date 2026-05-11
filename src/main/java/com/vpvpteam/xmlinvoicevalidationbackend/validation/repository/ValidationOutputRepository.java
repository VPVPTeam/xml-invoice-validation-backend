package com.vpvpteam.xmlinvoicevalidationbackend.validation.repository;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationOutputEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ValidationOutputRepository extends JpaRepository<ValidationOutputEntity, Long> {

    @EntityGraph(attributePaths = {"issues", "validationBatch"})
    Optional<ValidationOutputEntity> findByValidationBatch_BatchId(String batchId);
}