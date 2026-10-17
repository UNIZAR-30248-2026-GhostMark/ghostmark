package es.unizar.ghostmark.backend.partida;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartidaRepository extends JpaRepository<Partida, UUID> {

    boolean existsByCodigoInvitacion(String codigoInvitacion);
}
