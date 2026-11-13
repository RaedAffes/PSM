package com.psm.dto;

import java.math.BigDecimal;

public record TaskStats(
    long total,
    long todo,
    long inProgress,
    long done,
    long overdue,
    long urgent,
    BigDecimal totalHours
) {
}
