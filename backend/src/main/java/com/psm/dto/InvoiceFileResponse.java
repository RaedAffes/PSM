package com.psm.dto;

import com.psm.entity.InvoiceFile;
import java.time.Instant;

public record InvoiceFileResponse(
    Long id,
    String fileName,
    String contentType,
    long size,
    Instant createdAt,
    String downloadUrl
) {

    public static InvoiceFileResponse from(InvoiceFile file) {
        Long invoiceId = file.getInvoice().getId();
        return new InvoiceFileResponse(
            file.getId(),
            file.getFileName(),
            file.getContentType(),
            file.getSize(),
            file.getCreatedAt(),
            "/api/invoices/" + invoiceId + "/files/" + file.getId()
        );
    }
}
