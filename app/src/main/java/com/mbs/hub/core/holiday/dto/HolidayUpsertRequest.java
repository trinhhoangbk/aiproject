package com.mbs.hub.core.holiday.dto;

import com.mbs.hub.core.holiday.HolidayKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record HolidayUpsertRequest(
        @NotNull LocalDate date,
        @NotNull HolidayKind kind,
        @NotBlank String description
) {}
