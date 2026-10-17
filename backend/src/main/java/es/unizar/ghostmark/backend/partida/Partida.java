package es.unizar.ghostmark.backend.partida;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "partida")
public class Partida {

    @Id
    private UUID id;

    @Column(nullable = false, length = 40)
    private String nombre;

    @Column(name = "master_id")
    private UUID masterId;

    @Column(name = "master_juega", nullable = false)
    private boolean masterJuega;

    @Column(name = "inicio_en", nullable = false)
    private Instant inicioEn;

    @Column(name = "fin_en", nullable = false)
    private Instant finEn;

    @Column(name = "tipo_misiones", length = 10)
    private String tipoMisiones;

    @Column(name = "plazo_disputa_horas")
    private Short plazoDisputaHoras;

    @Column(name = "entradas_tardias", nullable = false)
    private boolean entradasTardias;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "codigo_invitacion", nullable = false, length = 8, unique = true)
    private String codigoInvitacion;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "actualizada_en", nullable = false)
    private Instant actualizadaEn;

    protected Partida() {
    }

    public Partida(UUID masterId, ConfiguracionPartida configuracion, String codigoInvitacion,
            Instant ahora) {
        this.id = UUID.randomUUID();
        this.nombre = configuracion.nombre();
        this.masterId = masterId;
        this.masterJuega = configuracion.masterJuega();
        this.inicioEn = configuracion.inicioEn();
        this.finEn = configuracion.finEn();
        this.tipoMisiones = configuracion.tipoMisiones();
        this.plazoDisputaHoras = configuracion.plazoDisputaHoras().shortValue();
        this.entradasTardias = configuracion.entradasTardias();
        this.codigoInvitacion = codigoInvitacion;
        this.estado = "EN_INSCRIPCION";
        this.creadaEn = ahora;
        this.actualizadaEn = ahora;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public UUID getMasterId() {
        return masterId;
    }

    public boolean isMasterJuega() {
        return masterJuega;
    }

    public Instant getInicioEn() {
        return inicioEn;
    }

    public Instant getFinEn() {
        return finEn;
    }

    public String getTipoMisiones() {
        return tipoMisiones;
    }

    public Short getPlazoDisputaHoras() {
        return plazoDisputaHoras;
    }

    public boolean isEntradasTardias() {
        return entradasTardias;
    }

    public String getCodigoInvitacion() {
        return codigoInvitacion;
    }

    public String getEstado() {
        return estado;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }
}
