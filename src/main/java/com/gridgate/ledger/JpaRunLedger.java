package com.gridgate.ledger;

import com.gridgate.domain.Run;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class JpaRunLedger implements RunLedger {

    private final RunEntityRepository repository;
    private final RunMapper mapper;

    public JpaRunLedger(RunEntityRepository repository, RunMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public Run save(Run run) {
        Objects.requireNonNull(run, "run");
        RunEntity entity = mapper.toEntity(run);
        RunEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Run> findById(UUID id) {
        Objects.requireNonNull(id, "id");
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Run> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
