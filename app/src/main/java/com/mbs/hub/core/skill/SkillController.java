package com.mbs.hub.core.skill;

import com.mbs.hub.core.skill.dto.SkillTagUpsertRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Skill taxonomy (DEC-007 — 2-level controlled list) and per-member assignments.
 */
@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillTagRepository tags;
    private final MemberSkillRepository memberSkills;

    public SkillController(SkillTagRepository tags, MemberSkillRepository memberSkills) {
        this.tags = tags; this.memberSkills = memberSkills;
    }

    // Taxonomy
    @GetMapping("/taxonomy")
    public List<SkillTag> listTaxonomy() { return tags.findAll(); }

    @PutMapping("/taxonomy")
    @Transactional
    public SkillTag upsertTag(@Valid @RequestBody SkillTagUpsertRequest req) {
        SkillKey k = new SkillKey(req.l1(), req.l2() == null ? "" : req.l2());
        SkillTag t = tags.findById(k).orElseGet(SkillTag::new);
        t.setKey(k);
        if (req.active() != null) t.setActive(req.active());
        return tags.save(t);
    }

    @DeleteMapping("/taxonomy")
    public void deleteTag(@RequestParam String l1, @RequestParam(required = false) String l2) {
        tags.deleteById(new SkillKey(l1, l2 == null ? "" : l2));
    }

    // Member skills
    @GetMapping("/members/{memberId}")
    public List<MemberSkill> memberSkills(@PathVariable UUID memberId) {
        return memberSkills.findByKey_MemberId(memberId);
    }

    @PostMapping("/members/{memberId}")
    @Transactional
    public MemberSkill addMemberSkill(@PathVariable UUID memberId,
                                      @Valid @RequestBody SkillTagUpsertRequest req) {
        SkillKey tagKey = new SkillKey(req.l1(), req.l2() == null ? "" : req.l2());
        if (tags.findById(tagKey).isEmpty())
            throw new NoSuchElementException("Skill tag " + req.l1() + "/" + req.l2()
                + " not in taxonomy — add it under /api/skills/taxonomy first");
        MemberSkill ms = new MemberSkill();
        ms.setKey(new MemberSkillKey(memberId, tagKey.getL1(), tagKey.getL2()));
        return memberSkills.save(ms);
    }

    @DeleteMapping("/members/{memberId}")
    @Transactional
    public void clearMemberSkills(@PathVariable UUID memberId) {
        memberSkills.deleteByKey_MemberId(memberId);
    }
}
