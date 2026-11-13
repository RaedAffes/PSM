package com.psm.dto;

import java.math.BigDecimal;

public record InvoiceStats(
    BigDecimal totalInvoiced,
    BigDecimal totalPaid,
    BigDecimal totalUnpaid,
    long paidCount,
    long unpaidCount,
    long overdueCount
) {
}
