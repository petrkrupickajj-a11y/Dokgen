package cz.petrk.dokgen.repository;

import cz.petrk.dokgen.entity.Klient;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA nam z tohoto rozhrani v runtime vyrobi celou
 * implementaci (findAll, findById, save, deleteById...) - nemusime
 * psat zadny SQL rucne.
 */
public interface KlientRepository extends JpaRepository<Klient, Long> {
}
