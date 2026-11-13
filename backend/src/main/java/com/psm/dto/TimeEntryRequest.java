package com.psm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TimeEntryRequest(
    @NotNull @DecimalMin(value = "0.01", message = "doit être supérieur à 0") BigDecimal hours,
    @Size(max = 500) String description,
    @NotNull LocalDate entryDate
) {
}
