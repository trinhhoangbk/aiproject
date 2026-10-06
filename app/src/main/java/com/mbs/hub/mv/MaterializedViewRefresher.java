package com.mbs.hub.mv;

import com.mbs.hub.core.member.Member;
import com.mbs.hub.core.member.MemberRepository;
import com.mbs.hub.mv.calc.AllocationRateCalculator;
import com.mbs.hub.mv.calc.OverdueCalculator;
import com.mbs.hub.sync.ProjectionUpdatedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * ADR-ARCH-009 refresher — PLAN-018 + PLAN-019.
 *
 * <ul>
 *   <li>Event-driven: a {@link ProjectionUpdatedEvent} schedules a refresh in 2 s;
 *       repeated events coalesce. Single-flight — only one refresh runs at a time.</li>
 *   <li>Nightly full rebuild at 02:30 Asia/Saigon as safety net.</li>
 * </ul>
 */
@Component
public class MaterializedViewRefresher {

    private static final Logger log = LoggerFactory.getLogger(MaterializedViewRefresher.class);
    private static final Duration DEBOUNCE = Duration.ofSeconds(2);

    private final MemberRepository members;
    private final AllocationRateCalculator allocation;
    private final OverdueCalculator overdue;
    private final AllocationRateRepository allocationRepo;
    private final MemberOverdueRepository overdueRepo;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "mv-refresher");
                t.setDaemon(true);
                return t;
            });
    private final AtomicReference<ScheduledFuture<?>> pending = new AtomicReference<>();

    private final Counter refreshCount;
    private final Timer   refreshDuration;

    public MaterializedViewRefresher(MemberRepository members,
                                     AllocationRateCalculator allocation,
                                     OverdueCalculator overdue,
                                     AllocationRateRepository allocationRepo,
                                     MemberOverdueRepository overdueRepo,
                                     MeterRegistry metrics) {
        this.members = members;
        this.allocation = allocation;
        this.overdue = overdue;
        this.allocationRepo = allocationRepo;
        this.overdueRepo = overdueRepo;
        this.refreshCount    = Counter.builder("mv_refresh_total").register(metrics);
        this.refreshDuration = Timer.builder("mv_refresh_duration_seconds").register(metrics);
    }

    /**
     * Debounced refresh on committed projection updates.
     *
     * Uses {@code AFTER_COMMIT} phase so a refresh never races with a half-rolled-back
     * transaction. Falls back to {@link EventListener} for non-transactional callers.
     */
    @TransactionalEventListener
    public void onProjectionUpdatedTx(ProjectionUpdatedEvent event) {
        schedule();
    }

    @EventListener
    public void onProjectionUpdated(ProjectionUpdatedEvent event) {
        schedule();
    }

    private void schedule() {
        ScheduledFuture<?> prev = pending.getAndSet(
                scheduler.schedule(this::refreshAll, DEBOUNCE.toMillis(), TimeUnit.MILLISECONDS)
        );
        if (prev != null) prev.cancel(false);
    }

    /** Nightly full rebuild (PLAN-019). */
    @Scheduled(cron = "0 30 2 * * *", zone = "Asia/Saigon")
    public void nightly() {
        log.info("MV nightly full rebuild starting");
        refreshAll();
    }

    @Transactional
    public void refreshAll() {
        Timer.Sample s = Timer.start();
        try {
            List<Member> all = members.findAll();
            for (Member m : all) refreshOne(m);
            refreshCount.increment();
        } catch (Exception e) {
            log.warn("MV refresh failed: {}", e.getMessage());
        } finally {
            s.stop(refreshDuration);
        }
    }

    private void refreshOne(Member m) {
        for (Horizon h : Horizon.values()) {
            AllocationRateRow row = allocation.compute(m, h);
            allocationRepo.save(row);
        }
        overdueRepo.deleteByMemberId(m.getId());
        for (MemberOverdueRow row : overdue.compute(m)) overdueRepo.save(row);
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }
}
