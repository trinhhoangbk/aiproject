package com.mbs.hub.mv;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllocationRateRepository
        extends JpaRepository<AllocationRateRow, AllocationRateKey> {

    List<AllocationRateRow> findByKey_MemberId(UUID memberId);
    List<AllocationRateRow> findByKey_Horizon(String horizon);
}
