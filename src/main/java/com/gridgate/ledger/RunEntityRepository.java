package com.gridgate.ledger;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RunEntityRepository extends JpaRepository<RunEntity, UUID> {
}
