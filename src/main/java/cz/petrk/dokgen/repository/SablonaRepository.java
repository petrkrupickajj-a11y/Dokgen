package cz.petrk.dokgen.repository;

import cz.petrk.dokgen.entity.Sablona;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SablonaRepository extends JpaRepository<Sablona, Long> {
    boolean existsByNazev(String nazev);
}
