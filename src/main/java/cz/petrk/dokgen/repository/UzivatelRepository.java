package cz.petrk.dokgen.repository;

import cz.petrk.dokgen.entity.Uzivatel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UzivatelRepository extends JpaRepository<Uzivatel, Long> {
    Optional<Uzivatel> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Uzivatel> findByAktivniFalseOrderByVytvorenoDneAsc();

    List<Uzivatel> findByAktivniTrueOrderByVytvorenoDneAsc();
}
