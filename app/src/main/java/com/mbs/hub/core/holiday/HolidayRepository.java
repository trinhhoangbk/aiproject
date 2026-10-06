package com.mbs.hub.core.holiday;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<Holiday, LocalDate> {
    List<Holiday> findByDateBetween(LocalDate from, LocalDate toInclusive);
    List<Holiday> findByKind(HolidayKind kind);
}
