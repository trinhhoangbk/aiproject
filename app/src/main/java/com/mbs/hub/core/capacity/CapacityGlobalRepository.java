package com.mbs.hub.core.capacity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Single-row table (id = 1 by CHECK constraint). JpaRepository is overkill
 * but keeps the pattern uniform across the three tiers.
 */
public interface CapacityGlobalRepository extends JpaRepository<CapacityGlobal, Short> {
    default CapacityGlobal getSingleton() {
        return findById((short) 1).orElseThrow(
            () -> new IllegalStateException("capacity_global row is missing — V001 seed failed")
        );
    }
}
