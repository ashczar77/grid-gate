package com.gridgate.ledger;

import com.gridgate.domain.Run;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Storage ledger interface for persisting and retrieving cascade runs.
 */
public interface RunLedger {

    Run save(Run run);

    Optional<Run> findById(UUID id);

    List<Run> findAll();
}
