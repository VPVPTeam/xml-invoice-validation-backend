package com.vpvpteam.xmlinvoicevalidationbackend.validation.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ValidationBatchDao {
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Map<String, OffsetDateTime> findBatchesByInvoiceId(String invoiceId) {
        Session session = entityManager.unwrap(Session.class);

        List<Object[]> rows = session.createNativeQuery("""
                SELECT
                    validation_batch.batch_id,
                    validation_batch.created_at
                FROM batch_invoice
                JOIN validation_batch
                    ON validation_batch.id = batch_invoice.validation_batch_id
                WHERE batch_invoice.invoice_id = :invoiceId
                ORDER BY validation_batch.created_at DESC, validation_batch.id DESC
                """, Object[].class)
                .setParameter("invoiceId", invoiceId)
                .getResultList();

        Map<String, OffsetDateTime> result = new LinkedHashMap<>();

        for (Object[] row : rows) {
            String batchId = (String) row[0];

            // Для PostgreSQL timestamptz обычно приходит как OffsetDateTime
            OffsetDateTime createdAt = (OffsetDateTime) row[1];

            result.put(batchId, createdAt);
        }

        return result;
    }
}

// TODO: ПОСЛЕ ЗАПУСКА ПРИЛОЖЕНИЯ ПРОВЕРИТЬ РАБОТАЕТ ЛИ ТАК ЖЕ С TUPLE
//@Repository
//public class ValidationBatchDao {
//
//    @PersistenceContext
//    private EntityManager entityManager;
//
//    @Transactional(readOnly = true)
//    public Map<String, OffsetDateTime> findBatchesByInvoiceId(String invoiceId) {
//        Session session = entityManager.unwrap(Session.class);
//
//        List<Tuple> rows = session.createNativeQuery("""
//                SELECT
//                    validation_batch.batch_id   AS batchId,
//                    validation_batch.created_at AS createdAt
//                FROM batch_invoice
//                JOIN validation_batch
//                  ON validation_batch.id = batch_invoice.validation_batch_id
//                WHERE batch_invoice.invoice_id = :invoiceId
//                ORDER BY validation_batch.created_at DESC, validation_batch.id DESC
//                """, Tuple.class)
//                .setParameter("invoiceId", invoiceId)
//                .getResultList();
//
//        Map<String, OffsetDateTime> result = new LinkedHashMap<>();
//
//        for (Tuple row : rows) {
//            String batchId = row.get("batchId", String.class);
//            OffsetDateTime createdAt = row.get("createdAt", OffsetDateTime.class);
//            result.put(batchId, createdAt);
//        }
//
//        return result;
//    }
//}