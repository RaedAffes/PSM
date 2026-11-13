package com.psm.repository;

import com.psm.entity.Invoice;
import com.psm.entity.InvoiceStatus;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @EntityGraph(attributePaths = { "client", "project" })
    @Override
    List<Invoice> findAll();

    @EntityGraph(attributePaths = { "client", "project" })
    List<Invoice> findByStatusOrderByIssueDateDescIdDesc(InvoiceStatus status);

    @EntityGraph(attributePaths = { "client", "project" })
    List<Invoice> findAllByOrderByIssueDateDescIdDesc();

    boolean existsByNumeroIgnoreCase(String numero);

    @Modifying
    @Query("update Invoice invoice set invoice.project = null where invoice.project.id = :projectId")
    void unlinkProject(@Param("projectId") Long projectId);
}
