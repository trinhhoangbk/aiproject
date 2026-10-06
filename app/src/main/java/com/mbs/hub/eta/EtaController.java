package com.mbs.hub.eta;

import com.mbs.hub.eta.dto.EtaView;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST surface for personal ETA — AC-004.1 / AC-004.2 / F-03.
 * MEMBER self-only; ADMIN/MANAGER any.
 */
@RestController
@RequestMapping("/api/eta")
public class EtaController {

    private final EtaService service;

    public EtaController(EtaService service) { this.service = service; }

    @GetMapping("/{memberId}")
    @PreAuthorize("""
            hasAnyRole('ADMIN','MANAGER')
            or (hasRole('MEMBER') and principal.memberId.toString() == #memberId.toString())
            """)
    public EtaView eta(@PathVariable UUID memberId) {
        return service.forecast(memberId);
    }
}
