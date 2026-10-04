package es.unizar.ghostmark.backend.sesion;

import java.util.Map;

public class DatosNoValidosException extends RuntimeException {

  private final Map<String, String> errores;

  public DatosNoValidosException(Map<String, String> errores) {
    super("Datos no válidos");
    this.errores = errores;
  }

  public Map<String, String> errores() {
    return errores;
  }
}
