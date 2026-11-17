package es.unizar.ghostmark.backend.partida;

import jakarta.persistence.LockModeType;
import java.time.Instant;
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

    @Query("select p.id from Partida p where p.estado = 'EN_INSCRIPCION'"
            + " and p.inicioEn <= :ahora order by p.id")
    List<UUID> pendientesDeArrancar(Instant ahora);

    /**
     * Algo que impide cerrar la partida (11a.2, «Cierre con disputas»): una disputa abierta o una
     * eliminación con el código de hace menos de 10 minutos, sin disputa y con la víctima aún
     * eliminada. Los 10 minutos son {@code ServicioDisputa.PLAZO_PARA_DISPUTAR}.
     */
    String PENDIENTE = "(exists (select 1 from disputa d where d.partida_id = p.id"
            + " and d.estado = 'ABIERTA') or exists (select 1 from evento e"
            + " join jugador_partida jp on jp.partida_id = e.partida_id"
            + " and jp.inscripcion_id = cast(e.datos->>'victimaId' as uuid)"
            + " where e.partida_id = p.id and e.tipo = 'JUGADOR_ELIMINADO'"
            + " and coalesce(e.datos->>'origen', 'CODIGO') = 'CODIGO'"
            + " and e.ocurrido_en > cast(:ahora as timestamptz) - interval '10 minutes'"
            + " and jp.estado = 'ELIMINADO'"
            + " and not exists (select 1 from disputa d where d.evento_eliminacion_id = e.id)))";

    /** Partidas en juego con la fecha de fin pasada o un único vivo, y sin nada pendiente. */
    @Query(value = "select p.id from partida p where p.estado = 'EN_JUEGO'"
            + " and (p.fin_en <= :ahora or (select count(*) from jugador_partida jp"
            + " where jp.partida_id = p.id and jp.estado = 'VIVO') = 1)"
            + " and not " + PENDIENTE + " order by p.id", nativeQuery = true)
    List<UUID> pendientesDeCerrar(Instant ahora);

    @Query(value = "select " + PENDIENTE + " from partida p where p.id = :partidaId",
            nativeQuery = true)
    boolean conCierrePendiente(UUID partidaId, Instant ahora);

    /** Bloquea las filas en orden de id, para guardar eventos sin chocar con otros. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Partida> findByMasterIdAndEstadoOrderByIdAsc(UUID masterId, String estado);

    boolean existsByCodigoInvitacion(String codigoInvitacion);

    Optional<Partida> findByCodigoInvitacion(String codigoInvitacion);
}
