package com.psm.dto;

import com.psm.entity.TimeEntry;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TimeEntryResponse(
    Long id,
    Long taskId,
    Long userId,
    String userName,
    BigDecimal hours,
    String description,
    LocalDate entryDate,
    Instant createdAt
) {

    public static TimeEntryResponse from(TimeEntry entry) {
        var user = entry.getUser();
        return new TimeEntryResponse(
            entry.getId(),
            entry.getTask().getId(),
            user == null ? null : user.getId(),
            user == null ? null : (user.getNom() + " " + user.getPrenom()).trim(),
            entry.getHours(),
            entry.getDescription(),
            entry.getEntryDate(),
            entry.getCreatedAt()
        );
    }
}
