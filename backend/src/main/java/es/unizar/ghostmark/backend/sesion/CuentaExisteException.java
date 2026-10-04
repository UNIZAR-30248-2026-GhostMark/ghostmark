package es.unizar.ghostmark.backend.sesion;

public class CuentaExisteException extends RuntimeException {

  private final String codigo;

  public CuentaExisteException(String codigo, String mensaje) {
    super(mensaje);
    this.codigo = codigo;
  }

  public String codigo() {
    return codigo;
  }
}
