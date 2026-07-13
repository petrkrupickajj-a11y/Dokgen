package cz.petrk.dokgen.repository;

import cz.petrk.dokgen.entity.ResetHesla;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResetHeslaRepository extends JpaRepository<ResetHesla, Long> {
    Optional<ResetHesla> findByTokenHash(String tokenHash);
}
