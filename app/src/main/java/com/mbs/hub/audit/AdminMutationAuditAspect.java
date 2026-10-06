package com.mbs.hub.audit;

import com.mbs.hub.security.HubUserDetails;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

/**
 * PLAN-031 — audit every POST/PUT/DELETE/PATCH against the admin-controller
 * package. The aspect runs around the controller method, so the audit row
 * captures the actor's memberId, the HTTP-verb-derived action, and the
 * verdict (OK on normal return, FAILED on thrown).
 *
 * <p>Why aspect, not an interceptor: an aspect sees the method name and arg
 * values before serialization and after validation, which is what the audit
 * payload wants.</p>
 */
@Aspect
@Component
public class AdminMutationAuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AdminMutationAuditAspect.class);

    private final AuditService audit;

    public AdminMutationAuditAspect(AuditService audit) { this.audit = audit; }

    @Around("""
            (within(com.mbs.hub.core.member..*)
             || within(com.mbs.hub.core.capacity..*)
             || within(com.mbs.hub.core.holiday..*)
             || within(com.mbs.hub.core.allowlist..*)
             || within(com.mbs.hub.core.skill..*))
            && (@annotation(org.springframework.web.bind.annotation.PostMapping)
             || @annotation(org.springframework.web.bind.annotation.PutMapping)
             || @annotation(org.springframework.web.bind.annotation.PatchMapping)
             || @annotation(org.springframework.web.bind.annotation.DeleteMapping))
            """)
    public Object audit(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String targetType = sig.getDeclaringType().getSimpleName()
                .replace("Controller", "").toLowerCase();
        String action = httpVerbOf(sig) + "_" + targetType.toUpperCase();
        UUID actor = currentMemberId();

        Map<String, Object> payload = new HashMap<>();
        payload.put("method", sig.getMethod().getName());
        payload.put("args", shortArgs(pjp.getArgs()));

        try {
            Object out = pjp.proceed();
            audit.ok(actor, action, targetType, extractTargetId(pjp.getArgs()), payload);
            return out;
        } catch (Throwable t) {
            Map<String, Object> failPayload = new HashMap<>(payload);
            failPayload.put("error", t.getClass().getSimpleName());
            failPayload.put("message", t.getMessage());
            audit.failed(actor, action, targetType, extractTargetId(pjp.getArgs()), failPayload);
            throw t;
        }
    }

    private static String httpVerbOf(MethodSignature sig) {
        var m = sig.getMethod();
        if (m.isAnnotationPresent(PostMapping.class))    return "CREATE";
        if (m.isAnnotationPresent(PutMapping.class))     return "UPDATE";
        if (m.isAnnotationPresent(PatchMapping.class))   return "PATCH";
        if (m.isAnnotationPresent(DeleteMapping.class))  return "DELETE";
        return "UNKNOWN";
    }

    private static UUID currentMemberId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return null;
        if (a.getPrincipal() instanceof HubUserDetails u) return u.getMemberId();
        return null;
    }

    private static String extractTargetId(Object[] args) {
        for (Object o : args) {
            if (o instanceof UUID u) return u.toString();
            if (o instanceof String s && s.length() < 128) return s;
        }
        return "-";
    }

    /** Keep the audit payload bounded — large request bodies get summarised. */
    private static Object shortArgs(Object[] args) {
        if (args == null || args.length == 0) return "[]";
        String s = java.util.Arrays.toString(args);
        return s.length() > 2048 ? s.substring(0, 2048) + "…" : s;
    }
}
