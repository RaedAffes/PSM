package com.psm.repository;

import com.psm.entity.InvoiceFile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceFileRepository extends JpaRepository<InvoiceFile, Long> {

    List<InvoiceFile> findByInvoiceIdOrderByCreatedAtAscIdAsc(Long invoiceId);

    List<InvoiceFile> findByInvoiceId(Long invoiceId);

    Optional<InvoiceFile> findByIdAndInvoiceId(Long id, Long invoiceId);
}
