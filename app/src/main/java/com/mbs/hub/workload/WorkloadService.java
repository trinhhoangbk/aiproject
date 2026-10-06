package com.mbs.hub.workload;

import com.mbs.hub.core.capacity.CapacityResolver;
import com.mbs.hub.core.capacity.EffectiveCapacity;
import com.mbs.hub.core.lockdeadline.LockDeadlineFlagRepository;
import com.mbs.hub.core.member.Member;
import com.mbs.hub.core.member.MemberRepository;
import com.mbs.hub.mv.AllocationRateKey;
import com.mbs.hub.mv.AllocationRateRepository;
import com.mbs.hub.mv.AllocationRateRow;
import com.mbs.hub.mv.Horizon;
import com.mbs.hub.sync.projection.IssueProjection;
import com.mbs.hub.sync.projection.IssueProjectionRepository;
import com.mbs.hub.workload.dto.WorkloadView;
import com.mbs.hub.workload.dto.WorkloadView.OverloadReason;
import com.mbs.hub.workload.dto.WorkloadView.OverloadView;
import com.mbs.hub.workload.dto.WorkloadView.ProjectBreakdown;
import com.mbs.hub.workload.dto.WorkloadView.UnestimatedSummary;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Composes a {@link WorkloadView} from:
 * <ul>
 *   <li>the member + 3-tier capacity (DEC-004);</li>
 *   <li>the pre-computed MV row for the horizon (DEC-011 band + AR);</li>
 *   <li>active issue projections for per-project breakdown (B-RULE-02 counts
 *       unestimated as 0.5 MD = 4 h);</li>
 *   <li>overload reasons per B-RULE-03 + DEC-010 three-criteria hard-deadline.</li>
 * </ul>
 */
@Service
public class WorkloadService {

    static final BigDecimal NO_ESTIMATE_HOURS = new BigDecimal("4");
    static final BigDecimal HALF_MD = new BigDecimal("0.5");

    private final MemberRepository members;
    private final CapacityResolver capacity;
    private final AllocationRateRepository mvRate;
    private final IssueProjectionRepository issues;
    private final LockDeadlineFlagRepository lockDeadlineFlags;
    private final Clock clock;

    public WorkloadService(MemberRepository members, CapacityResolver capacity,
                           AllocationRateRepository mvRate,
                           IssueProjectionRepository issues,
                           LockDeadlineFlagRepository lockDeadlineFlags,
                           Clock clock) {
        this.members = members;
        this.capacity = capacity;
        this.mvRate = mvRate;
        this.issues = issues;
        this.lockDeadlineFlags = lockDeadlineFlags;
        this.clock = clock;
    }

    public WorkloadView loadWorkload(UUID memberId, String windowParam, LocalDate anchorParam) {
        Member m = members.findById(memberId).orElseThrow(() ->
            new NoSuchElementException("Member " + memberId + " not found"));
        Horizon horizon = parseHorizon(windowParam);
        LocalDate anchor = anchorParam == null ? LocalDate.now(clock) : anchorParam;
        LocalDate winStart = horizon.windowStart(anchor);

        AllocationRateRow mvRow = mvRate
                .findById(new AllocationRateKey(memberId, horizon.wire(), winStart))
                .orElse(null);

        List<IssueProjection> active = m.getJiraAccountId() == null
                ? List.of()
                : issues.findByAssigneeAccountIdAndStatusCategoryNotAndAllowListOkTrue(
                        m.getJiraAccountId(), "done");

        EffectiveCapacity ec = capacity.resolveOn(memberId, anchor);

        List<ProjectBreakdown> breakdown = projectBreakdown(active);
        UnestimatedSummary unestimated = unestimated(active);
        OverloadView overload = overload(memberId, active, ec);

        BigDecimal standardMd = mvRow == null ? BigDecimal.ZERO : mvRow.getStandardMd();
        BigDecimal committedMd = mvRow == null ? BigDecimal.ZERO : mvRow.getCommittedMd();
        BigDecimal availableMd = mvRow == null ? BigDecimal.ZERO : mvRow.getAvailableMd();
        BigDecimal ar          = mvRow == null ? BigDecimal.ZERO : mvRow.getAllocationRate();
        String band            = mvRow == null ? "dark_green" : mvRow.getBand();

        return new WorkloadView(memberId, m.getDisplayName(), windowParam, anchor,
                standardMd, committedMd, availableMd, ar, band,
                overload, breakdown, unestimated);
    }

    public OverloadView loadOverload(UUID memberId) {
        Member m = members.findById(memberId).orElseThrow(() ->
            new NoSuchElementException("Member " + memberId + " not found"));
        LocalDate today = LocalDate.now(clock);
        EffectiveCapacity ec = capacity.resolveOn(memberId, today);
        List<IssueProjection> active = m.getJiraAccountId() == null
                ? List.of()
                : issues.findByAssigneeAccountIdAndStatusCategoryNotAndAllowListOkTrue(
                        m.getJiraAccountId(), "done");
        return overload(memberId, active, ec);
    }

    // --------------------------------------------------------------------
    // internals
    // --------------------------------------------------------------------

    private static Horizon parseHorizon(String w) {
        if (w == null) return Horizon.THIS_WEEK;
        return switch (w) {
            case "day", "week", "this_week"             -> Horizon.THIS_WEEK;
            case "2weeks", "next_2_weeks"                -> Horizon.NEXT_2_WEEKS;
            case "month", "next_month"                   -> Horizon.NEXT_MONTH;
            default -> throw new IllegalArgumentException("Unknown window: " + w);
        };
    }

    private List<ProjectBreakdown> projectBreakdown(List<IssueProjection> active) {
        Map<String, int[]>        counts = new HashMap<>();   // projectKey → {count}
        Map<String, BigDecimal>   hours  = new HashMap<>();   // projectKey → sum remaining
        BigDecimal totalCount = BigDecimal.ZERO;
        BigDecimal totalHours = BigDecimal.ZERO;
        for (IssueProjection i : active) {
            BigDecimal h = i.getRemainingEstimateH();
            if (h == null) h = NO_ESTIMATE_HOURS;
            counts.computeIfAbsent(i.getProjectKey(), k -> new int[]{0})[0]++;
            hours.merge(i.getProjectKey(), h, BigDecimal::add);
            totalCount = totalCount.add(BigDecimal.ONE);
            totalHours = totalHours.add(h);
        }
        BigDecimal denomC = totalCount.signum() == 0 ? BigDecimal.ONE : totalCount;
        BigDecimal denomH = totalHours.signum() == 0 ? BigDecimal.ONE : totalHours;
        List<ProjectBreakdown> out = new ArrayList<>();
        for (var e : hours.entrySet()) {
            int ic = counts.get(e.getKey())[0];
            BigDecimal h = e.getValue();
            out.add(new ProjectBreakdown(
                    e.getKey(), ic, h,
                    h.divide(denomH, 2, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(ic).divide(denomC, 2, RoundingMode.HALF_UP)
            ));
        }
        out.sort((a, b) -> b.remainingH().compareTo(a.remainingH()));
        return out;
    }

    private UnestimatedSummary unestimated(List<IssueProjection> active) {
        int n = (int) active.stream().filter(i -> i.getRemainingEstimateH() == null).count();
        return new UnestimatedSummary(n, HALF_MD);
    }

    /** B-RULE-03 + DEC-010 three-criteria. */
    OverloadView overload(UUID memberId, List<IssueProjection> active, EffectiveCapacity ec) {
        List<OverloadReason> reasons = new ArrayList<>();
        BigDecimal dailyCap   = ec.dailyHours();
        BigDecimal weeklyCap  = ec.weeklyHours();

        // Week-level
        BigDecimal weekCommitted = active.stream()
                .map(i -> i.getRemainingEstimateH() == null ? NO_ESTIMATE_HOURS : i.getRemainingEstimateH())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (weekCommitted.compareTo(weeklyCap) > 0) {
            reasons.add(new OverloadReason("week", null, weekCommitted, weeklyCap, null));
        }

        // Day-level — group by due_date, test both >dailyCap AND hard-deadline criteria met
        Set<String> lockedIssueKeys = active.stream()
                .map(IssueProjection::getIssueKey)
                .filter(lockDeadlineFlags::existsById)
                .collect(Collectors.toSet());

        Map<LocalDate, BigDecimal> hoursByDue = new HashMap<>();
        Map<LocalDate, List<IssueProjection>> issuesByDue = new HashMap<>();
        for (IssueProjection i : active) {
            if (i.getDueDate() == null) continue;
            BigDecimal h = i.getRemainingEstimateH() == null ? NO_ESTIMATE_HOURS : i.getRemainingEstimateH();
            hoursByDue.merge(i.getDueDate(), h, BigDecimal::add);
            issuesByDue.computeIfAbsent(i.getDueDate(), k -> new ArrayList<>()).add(i);
        }
        for (var e : hoursByDue.entrySet()) {
            if (e.getValue().compareTo(dailyCap) <= 0) continue;
            List<String> criteriaUsed = hardDeadlineCriteria(issuesByDue.get(e.getKey()), lockedIssueKeys);
            if (criteriaUsed.isEmpty()) continue;    // not "cannot-reschedule"
            reasons.add(new OverloadReason("day", e.getKey(), e.getValue(), dailyCap, criteriaUsed));
        }
        String flag = reasons.isEmpty() ? "green" : "red";
        return new OverloadView(flag, reasons);
    }

    /** DEC-010 — returns which criteria triggered among {fix_version_release, priority_blocker, hub_lock_flag}. */
    private List<String> hardDeadlineCriteria(List<IssueProjection> issuesForDay, Set<String> lockedIssueKeys) {
        List<String> hits = new ArrayList<>();
        boolean fv = false, prio = false, lock = false;
        for (IssueProjection i : issuesForDay) {
            if (!fv && i.getFixVersionReleaseDate() != null) {
                hits.add("fix_version_release");
                fv = true;
            }
            String p = i.getPriority();
            boolean prioritised = p != null && (p.equalsIgnoreCase("Blocker") || p.equalsIgnoreCase("Critical"));
            boolean labelled    = i.getLabels() != null
                    && java.util.Arrays.asList(i.getLabels()).contains("Hard-Deadline");
            if (!prio && (prioritised || labelled)) {
                hits.add("priority_blocker");
                prio = true;
            }
            if (!lock && lockedIssueKeys.contains(i.getIssueKey())) {
                hits.add("hub_lock_flag");
                lock = true;
            }
        }
        return hits;
    }
}
