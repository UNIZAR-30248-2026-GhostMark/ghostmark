package es.unizar.ghostmark.backend.partida;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartidaRepository extends JpaRepository<Partida, UUID> {

    List<Partida> findByMasterId(UUID masterId);

    List<Partida> findByMasterIdAndEstado(UUID masterId, String estado);

    boolean existsByCodigoInvitacion(String codigoInvitacion);
}