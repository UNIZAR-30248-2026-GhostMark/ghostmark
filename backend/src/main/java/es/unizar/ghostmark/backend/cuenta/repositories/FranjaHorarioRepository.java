package es.unizar.ghostmark.backend.cuenta.repositories;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import es.unizar.ghostmark.backend.cuenta.models.FranjaHorario;
import es.unizar.ghostmark.backend.cuenta.models.FranjaHorarioId;

public interface FranjaHorarioRepository extends JpaRepository<FranjaHorario, FranjaHorarioId> {

    @Query("""
        select f from FranjaHorario f
        where f.id.cuentaId = :cuentaId
        order by f.id.dia, f.id.horaInicio
        """)
    List<FranjaHorario> buscarPorCuentaId(@Param("cuentaId") UUID cuentaId);
}