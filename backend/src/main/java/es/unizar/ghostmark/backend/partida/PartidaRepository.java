package es.unizar.ghostmark.backend.partida;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PartidaRepository extends JpaRepository<Partida, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Partida p where p.codigoInvitacion = :codigo")
    Optional<Partida> bloquearPorCodigo(String codigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Partida p where p.id = :id")
    Optional<Partida> bloquearPorId(UUID id);

    @Query(value = "select p.* from partida p where p.master_id = :cuentaId or exists"
            + " (select 1 from inscripcion i where i.partida_id = p.id and i.cuenta_id = :cuentaId)"
            + " order by p.creada_en, p.id", nativeQuery = true)
    List<Partida> visiblesPara(UUID cuentaId);

    List<Partida> findByMasterId(UUID masterId);

    /** Bloquea las filas en orden de id, para guardar eventos sin chocar con otros. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Partida> findByMasterIdAndEstadoOrderByIdAsc(UUID masterId, String estado);

    boolean existsByCodigoInvitacion(String codigoInvitacion);

    Optional<Partida> findByCodigoInvitacion(String codigoInvitacion);
}
