package cz.petrk.dokgen.repository;

import cz.petrk.dokgen.entity.VygenerovanyDokument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface VygenerovanyDokumentRepository extends JpaRepository<VygenerovanyDokument, Long> {
    List<VygenerovanyDokument> findAllByOrderByVytvorenoDneDesc();

    List<VygenerovanyDokument> findAllByVytvorenoDneBefore(LocalDateTime cas);
}
