package com.mbs.hub.mv;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberOverdueRepository
        extends JpaRepository<MemberOverdueRow, MemberOverdueKey> {

    List<MemberOverdueRow> findByKey_MemberId(UUID memberId);

    @Modifying
    @Query("DELETE FROM MemberOverdueRow r WHERE r.key.memberId = :memberId")
    void deleteByMemberId(@Param("memberId") UUID memberId);
}
