package com.mbs.hub.core.pipeline;

import com.mbs.hub.core.pipeline.dto.PipelineRequest;
import com.mbs.hub.core.pipeline.dto.PipelineView;
import com.mbs.hub.core.skill.SkillKey;
import com.mbs.hub.core.skill.SkillTagRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pipeline CRUD — PLAN-024 / REQ-005 / AC-005.*.
 *
 * <p>Create/update validate the request's {@code requiredSkills} against the
 * DEC-007 {@code core.skill_taxonomy} list — a skill the taxonomy does not know
 * is rejected before the row is written, consistent with the FK on
 * {@code core.pipeline_required_skill}.</p>
 */
@Service
public class PipelineService {

    private final PipelineProjectRepository pipelines;
    private final PipelineRequiredSkillRepository requiredSkills;
    private final SkillTagRepository skillTags;

    public PipelineService(PipelineProjectRepository pipelines,
                           PipelineRequiredSkillRepository requiredSkills,
                           SkillTagRepository skillTags) {
        this.pipelines = pipelines;
        this.requiredSkills = requiredSkills;
        this.skillTags = skillTags;
    }

    public List<PipelineView> list(PipelineState state) {
        List<PipelineProject> rows = state != null
                ? pipelines.findByStateOrderByTargetDeadlineAsc(state)
                : pipelines.findAll();
        return rows.stream().map(this::toView).toList();
    }

    public PipelineView get(UUID id) {
        return toView(pipelines.findById(id)
                .orElseThrow(() -> new NoSuchElementException("pipeline " + id + " not found")));
    }

    @Transactional
    public PipelineView create(PipelineRequest req, UUID actor) {
        validateSkills(req);
        PipelineProject p = new PipelineProject();
        apply(p, req);
        p.setCreatedBy(actor);
        p.setState(PipelineState.DRAFT);
        p = pipelines.save(p);
        writeSkills(p.getId(), req);
        return toView(p);
    }

    @Transactional
    public PipelineView update(UUID id, PipelineRequest req) {
        validateSkills(req);
        PipelineProject p = pipelines.findById(id)
                .orElseThrow(() -> new NoSuchElementException("pipeline " + id + " not found"));
        apply(p, req);
        p = pipelines.save(p);
        requiredSkills.deleteByKey_PipelineId(id);
        writeSkills(id, req);
        return toView(p);
    }

    @Transactional
    public void delete(UUID id) {
        requiredSkills.deleteByKey_PipelineId(id);
        pipelines.deleteById(id);
    }

    @Transactional
    public PipelineView transition(UUID id, PipelineState next) {
        PipelineProject p = pipelines.findById(id)
                .orElseThrow(() -> new NoSuchElementException("pipeline " + id + " not found"));
        p.setState(next);
        return toView(pipelines.save(p));
    }

    // -------------------------------------------------------------------

    private void validateSkills(PipelineRequest req) {
        for (var s : req.requiredSkills()) {
            SkillKey k = new SkillKey(s.l1(), s.safeL2());
            if (!skillTags.existsById(k)) {
                throw new IllegalArgumentException(
                        "skill not in taxonomy (DEC-007): " + s.l1() + "/" + s.safeL2());
            }
        }
    }

    private void apply(PipelineProject p, PipelineRequest req) {
        p.setName(req.name());
        p.setObjective(req.objective());
        p.setTargetDeadline(req.targetDeadline());
        p.setTotalEstimatedMd(req.totalEstimatedMd());
    }

    private void writeSkills(UUID pipelineId, PipelineRequest req) {
        for (var s : req.requiredSkills()) {
            PipelineRequiredSkill row = new PipelineRequiredSkill();
            row.setKey(new PipelineRequiredSkillKey(pipelineId, s.l1(), s.safeL2()));
            requiredSkills.save(row);
        }
    }

    private PipelineView toView(PipelineProject p) {
        List<PipelineView.SkillRef> skills = requiredSkills.findByKey_PipelineId(p.getId()).stream()
                .map(r -> new PipelineView.SkillRef(
                        r.getKey().getL1() + (r.getKey().getL2().isBlank() ? "" : " / " + r.getKey().getL2()),
                        r.getKey().getL1(),
                        r.getKey().getL2()))
                .toList();
        return new PipelineView(
                p.getId(), p.getName(), p.getObjective(),
                p.getTargetDeadline(), p.getTotalEstimatedMd(), p.getState(),
                skills, p.getCreatedBy(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
