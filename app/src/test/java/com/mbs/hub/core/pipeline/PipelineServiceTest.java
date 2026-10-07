package com.mbs.hub.core.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mbs.hub.core.pipeline.dto.PipelineRequest;
import com.mbs.hub.core.pipeline.dto.PipelineView;
import com.mbs.hub.core.skill.SkillTagRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** TC-PL-01..02 — AC-005.1 / DEC-007. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PipelineServiceTest {

    @Mock PipelineProjectRepository pipelines;
    @Mock PipelineRequiredSkillRepository requiredSkills;
    @Mock SkillTagRepository skillTags;

    static PipelineRequest request(String l1) {
        return new PipelineRequest("Core banking API", "Expose MPoint API",
                LocalDate.of(2026, 11, 30), new BigDecimal("12.5"),
                List.of(new PipelineRequest.SkillRef(l1, null)));
    }

    @Test
    void TC_PL_01_createStoresDraftWithSkills() {
        UUID actor = UUID.randomUUID();
        when(skillTags.existsById(any())).thenReturn(true);
        when(pipelines.save(any())).thenAnswer(inv -> {
            PipelineProject p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(requiredSkills.findByKey_PipelineId(any())).thenReturn(List.of());

        PipelineView v = new PipelineService(pipelines, requiredSkills, skillTags)
                .create(request("BACKEND"), actor);

        assertThat(v.state()).isEqualTo(PipelineState.DRAFT);
        assertThat(v.createdBy()).isEqualTo(actor);
        verify(requiredSkills, times(1)).save(any());
    }

    @Test
    void TC_PL_02_unknownSkillRejectedAndNothingSaved() {
        when(skillTags.existsById(any())).thenReturn(false);
        PipelineService service = new PipelineService(pipelines, requiredSkills, skillTags);

        assertThatThrownBy(() -> service.create(request("COBOL"), UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("COBOL");
        verify(pipelines, never()).save(any());
        verify(requiredSkills, never()).save(any());
    }
}
