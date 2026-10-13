package es.unizar.ghostmark.backend.cuenta.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class FranjaHorarioId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "cuenta_id", nullable = false)
    private UUID cuentaId;

    @Column(name = "dia", nullable = false)
    private short dia;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    protected FranjaHorarioId() {
    }

    public FranjaHorarioId(UUID cuentaId, short dia, LocalTime horaInicio) {
        this.cuentaId = cuentaId;
        this.dia = dia;
        this.horaInicio = horaInicio;
    }

    public UUID getCuentaId() {
        return cuentaId;
    }

    public short getDia() {
        return dia;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
        return true;
        }
        if (!(otro instanceof FranjaHorarioId id)) {
        return false;
        }
        return dia == id.dia
            && Objects.equals(cuentaId, id.cuentaId)
            && Objects.equals(horaInicio, id.horaInicio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cuentaId, dia, horaInicio);
    }
}