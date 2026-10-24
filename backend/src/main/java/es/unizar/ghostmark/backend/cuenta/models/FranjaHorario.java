package es.unizar.ghostmark.backend.cuenta.models;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "franja_horario")
public class FranjaHorario {

    @EmbeddedId
    private FranjaHorarioId id;

    protected FranjaHorario() {
    }

    public FranjaHorario(UUID cuentaId, short dia, LocalTime horaInicio) {
        this.id = new FranjaHorarioId(cuentaId, dia, horaInicio);
    }

    public FranjaHorarioId getId() {
        return id;
    }
}
