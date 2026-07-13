package cz.petrk.dokgen.repository;

import cz.petrk.dokgen.entity.SmazanaVestavenaSablona;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmazanaVestavenaSablonaRepository extends JpaRepository<SmazanaVestavenaSablona, Long> {
    boolean existsByNazev(String nazev);
}
