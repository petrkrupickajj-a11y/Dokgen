package cz.petrk.dokgen.repository;

import cz.petrk.dokgen.entity.SablonaVerze;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SablonaVerzeRepository extends JpaRepository<SablonaVerze, Long> {
    List<SablonaVerze> findBySablonaIdOrderByUlozenoDneDesc(Long sablonaId);
}
