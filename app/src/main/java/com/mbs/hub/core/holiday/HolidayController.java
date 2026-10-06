package com.mbs.hub.core.holiday;

import com.mbs.hub.core.holiday.dto.HolidayUpsertRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Admin / Operations Lead co-ownership per DEC-003. RBAC in M8.
 */
@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    private final HolidayRepository holidays;

    public HolidayController(HolidayRepository holidays) { this.holidays = holidays; }

    @GetMapping
    public List<Holiday> list(@RequestParam(required = false) LocalDate from,
                              @RequestParam(required = false) LocalDate to) {
        if (from != null && to != null) return holidays.findByDateBetween(from, to);
        return holidays.findAll();
    }

    @GetMapping("/{date}")
    public Holiday get(@PathVariable LocalDate date) {
        return holidays.findById(date).orElseThrow(
            () -> new NoSuchElementException("Holiday on " + date + " not found"));
    }

    @PutMapping("/{date}")
    @Transactional
    public Holiday upsert(@PathVariable LocalDate date, @Valid @RequestBody HolidayUpsertRequest req) {
        if (!date.equals(req.date()))
            throw new IllegalArgumentException("Path date must match body date");
        Holiday h = holidays.findById(date).orElseGet(Holiday::new);
        h.setDate(req.date());
        h.setKind(req.kind());
        h.setDescription(req.description());
        return holidays.save(h);
    }

    @DeleteMapping("/{date}")
    public void delete(@PathVariable LocalDate date) { holidays.deleteById(date); }
}
