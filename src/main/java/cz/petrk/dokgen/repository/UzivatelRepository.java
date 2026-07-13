package cz.petrk.dokgen.repository;

import cz.petrk.dokgen.entity.Uzivatel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UzivatelRepository extends JpaRepository<Uzivatel, Long> {
    Optional<Uzivatel> findByJmeno(String jmeno);

    boolean existsByJmeno(String jmeno);
}
