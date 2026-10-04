package es.unizar.ghostmark.backend.cuenta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "token_refresco")
public class TokenRefresco {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cuenta_id", nullable = false)
  private Cuenta cuenta;

  @Column(name = "hash_token", nullable = false, length = 64, unique = true)
  private String hashToken;

  @Column(name = "caduca_en", nullable = false)
  private Instant caducaEn;

  @Column(name = "revocado_en")
  private Instant revocadoEn;

  @Column(name = "creado_en", nullable = false)
  private Instant creadoEn;

  protected TokenRefresco() {
  }

  public TokenRefresco(Cuenta cuenta, String hashToken, Instant caducaEn, Instant ahora) {
    this.id = UUID.randomUUID();
    this.cuenta = cuenta;
    this.hashToken = hashToken;
    this.caducaEn = caducaEn;
    this.creadoEn = ahora;
  }

  public UUID getId() {
    return id;
  }

  public Cuenta getCuenta() {
    return cuenta;
  }

  public String getHashToken() {
    return hashToken;
  }

  public Instant getCaducaEn() {
    return caducaEn;
  }

  public Instant getRevocadoEn() {
    return revocadoEn;
  }

  public boolean valePara(Instant ahora) {
    return revocadoEn == null && caducaEn.isAfter(ahora);
  }

  public void revocar(Instant ahora) {
    this.revocadoEn = ahora;
  }
}
