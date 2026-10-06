package com.mbs.hub.core.lockdeadline;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LockDeadlineFlagRepository extends JpaRepository<LockDeadlineFlag, String> {}
