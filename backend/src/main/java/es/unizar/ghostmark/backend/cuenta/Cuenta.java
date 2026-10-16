package es.unizar.ghostmark.backend.cuenta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cuenta")
public class Cuenta {

    @Id
    private UUID id;

    @Column(nullable = false, length = 20)
    private String alias;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "hash_contrasena", nullable = false, length = 60)
    private String hashContrasena;

    @Column(name = "ubicacion_aceptada_en")
    private Instant ubicacionAceptadaEn;

    @Column(name = "aviso_disponibilidad_aceptado_en")
    private Instant avisoDisponibilidadAceptadoEn;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "actualizada_en", nullable = false)
    private Instant actualizadaEn;

    protected Cuenta() {
    }

    public Cuenta(String alias, String email, String hashContrasena, Instant ahora) {
        this.id = UUID.randomUUID();
        this.alias = alias;
        this.email = email.toLowerCase();
        this.hashContrasena = hashContrasena;
        this.creadaEn = ahora;
        this.actualizadaEn = ahora;
    }

    public UUID getId() {
        return id;
    }

    public String getAlias() {
        return alias;
    }

    public String getEmail() {
        return email;
    }

    public String getHashContrasena() {
        return hashContrasena;
    }

    public Instant getUbicacionAceptadaEn() {
        return ubicacionAceptadaEn;
    }

    public Instant getAvisoDisponibilidadAceptadoEn() {
        return avisoDisponibilidadAceptadoEn;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }
}
