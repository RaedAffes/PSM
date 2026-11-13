package com.psm.dto;

import com.psm.entity.Invoice;
import com.psm.entity.InvoiceStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record InvoiceResponse(
    Long id,
    String numero,
    Long clientId,
    String clientName,
    Long projectId,
    String projectLibelle,
    InvoiceStatus status,
    LocalDate issueDate,
    LocalDate dueDate,
    LocalDate paidDate,
    BigDecimal amount,
    BigDecimal taxRate,
    BigDecimal totalAmount,
    String paymentMethod,
    String notes,
    boolean overdue,
    Instant createdAt,
    Instant updatedAt
) {

    public static InvoiceResponse from(Invoice invoice) {
        var client = invoice.getClient();
        var project = invoice.getProject();
        boolean overdue = invoice.getStatus() == InvoiceStatus.UNPAID
            && invoice.getDueDate() != null
            && invoice.getDueDate().isBefore(LocalDate.now());

        return new InvoiceResponse(
            invoice.getId(),
            invoice.getNumero(),
            client == null ? null : client.getId(),
            client == null ? null : client.getNom(),
            project == null ? null : project.getId(),
            project == null ? null : project.getLibelle(),
            invoice.getStatus(),
            invoice.getIssueDate(),
            invoice.getDueDate(),
            invoice.getPaidDate(),
            invoice.getAmount(),
            invoice.getTaxRate(),
            invoice.getTotalAmount(),
            invoice.getPaymentMethod(),
            invoice.getNotes(),
            overdue,
            invoice.getCreatedAt(),
            invoice.getUpdatedAt()
        );
    }
}
