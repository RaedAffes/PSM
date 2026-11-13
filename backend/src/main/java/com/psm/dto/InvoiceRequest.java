package com.psm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceRequest {

    private String numero;

    @NotNull
    private Long clientId;

    private Long projectId;

    @NotNull
    private LocalDate issueDate;

    private LocalDate dueDate;

    @NotNull
    @DecimalMin(value = "0.0", message = "le montant doit être positif")
    private BigDecimal amount;

    @DecimalMin(value = "0.0", message = "le taux de TVA doit être positif")
    private BigDecimal taxRate;

    private String paymentMethod;

    private String notes;
}
