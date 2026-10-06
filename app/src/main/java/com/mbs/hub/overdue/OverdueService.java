package com.mbs.hub.overdue;

import com.mbs.hub.mv.MemberOverdueRepository;
import com.mbs.hub.mv.MemberOverdueRow;
import com.mbs.hub.overdue.dto.OverdueView;
import com.mbs.hub.overdue.dto.WarningView;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * REQ-003 reader. Everything comes from {@code mv.mv_member_overdue} which the
 * M4 refresher keeps current. Overdue rows carry {@code daysOverdue > 0} and a
 * band; TPR/early-warning rows carry {@code daysOverdue == 0} with a non-NONE
 * {@code tprBand}.
 */
@Service
public class OverdueService {

    private final MemberOverdueRepository repo;

    public OverdueService(MemberOverdueRepository repo) { this.repo = repo; }

    public List<OverdueView> overdue(UUID memberId) {
        List<MemberOverdueRow> rows = memberId == null ? repo.findAll() : repo.findByKey_MemberId(memberId);
        return rows.stream()
                .filter(r -> r.getDaysOverdue() > 0)
                .map(r -> new OverdueView(
                        r.getKey().getIssueKey(), r.getProjectKey(),
                        r.getKey().getMemberId(), r.getDueDate(),
                        r.getDaysOverdue(), r.getOverdueBand(), r.getRemainingH()))
                .sorted((a, b) -> Integer.compare(b.daysOverdue(), a.daysOverdue()))
                .collect(Collectors.toList());
    }

    public List<WarningView> warnings(UUID memberId) {
        List<MemberOverdueRow> rows = memberId == null ? repo.findAll() : repo.findByKey_MemberId(memberId);
        return rows.stream()
                .filter(r -> r.getDaysOverdue() == 0 && !"none".equals(r.getTprBand()))
                .map(r -> new WarningView(
                        r.getKey().getIssueKey(), r.getProjectKey(),
                        r.getKey().getMemberId(), r.getDueDate(),
                        r.getRemainingH(), r.getTpr(), r.getTprBand()))
                .sorted((a, b) -> b.tpr() == null ? 1 : a.tpr() == null ? -1 : b.tpr().compareTo(a.tpr()))
                .collect(Collectors.toList());
    }
}
