package es.unizar.ghostmark.backend.partida;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface PartidaRepository extends JpaRepository<Partida, UUID> {

    List<Partida> findByMasterId(UUID masterId);

    /** Bloquea las filas en orden de id, para guardar eventos sin chocar con otros. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Partida> findByMasterIdAndEstadoOrderByIdAsc(UUID masterId, String estado);

    boolean existsByCodigoInvitacion(String codigoInvitacion);

    Optional<Partida> findByCodigoInvitacion(String codigoInvitacion);
}
